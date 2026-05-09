import React, { useEffect, useMemo, useState } from 'react';
import { GridCell } from './GridCell';
import { Notification } from '@vaadin/react-components/Notification.js';
import {
  BASE_TEMPLATE_LOCKED_MESSAGE,
  getMutationErrorMessage,
  isBaseTemplateLocked,
  isPublished,
  refreshSchedule,
  scheduleData,
  scheduleLoading,
  selectedCourseFilter,
  type CourseFilter
} from '../store/app-state';
import { ScheduleEndpoint } from '../generated/endpoints';
import AssignLessonDialog from './AssignLessonDialog';

const dayNames: Record<string, string> = {
  MONDAY: 'понеділок',
  TUESDAY: 'вівторок',
  WEDNESDAY: 'середа',
  THURSDAY: 'четвер',
  FRIDAY: 'п\'ятниця',
  SATURDAY: 'субота',
  SUNDAY: 'неділя'
};

const COURSE_FILTERS = [1, 2, 3, 4] as const;

export const ScheduleGrid: React.FC = () => {
  const data = scheduleData.value;
  const loading = scheduleLoading.value;
  const published = isPublished.value;
  const baseTemplateLocked = isBaseTemplateLocked.value;
  const selectedCourse = selectedCourseFilter.value;

  const [assignDialogState, setAssignDialogState] = useState<{
    opened: boolean;
    day: string;
    lessonNum: number;
    groupId: number;
    timeslotId?: number;
  }>({
    opened: false,
    day: 'MONDAY',
    lessonNum: 1,
    groupId: 0
  });

  useEffect(() => {
    refreshSchedule();
  }, []);

  const allGroups = useMemo(() => {
    return [...(data?.groups || [])]
      .filter(g => !!g)
      .sort((a, b) => a.name.localeCompare(b.name));
  }, [data?.groups]);

  const courseCounts = useMemo(() => {
    const counts = new Map<number, number>();
    allGroups.forEach((group: any) => {
      const course = Number(group.course);
      if (Number.isInteger(course)) {
        counts.set(course, (counts.get(course) || 0) + 1);
      }
    });
    return counts;
  }, [allGroups]);

  const availableCourses = COURSE_FILTERS.filter(course => courseCounts.has(course));
  const effectiveCourse: CourseFilter =
    selectedCourse === 'ALL' || courseCounts.has(selectedCourse)
      ? selectedCourse
      : availableCourses[0] ?? 'ALL';
  const groups = effectiveCourse === 'ALL'
    ? allGroups
    : allGroups.filter((group: any) => Number(group.course) === effectiveCourse);

  useEffect(() => {
    if (selectedCourse !== effectiveCourse) {
      selectedCourseFilter.value = effectiveCourse;
    }
  }, [selectedCourse, effectiveCourse]);

  if (loading) return <div className="p-8 text-center text-gray-500 animate-pulse font-serif">Завантаження матриці розкладу...</div>;
  if (!data) return <div className="p-8 text-red-500 text-center font-bold font-serif">Помилка: дані розкладу не завантажено</div>;

  const days = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'];
  const lessonNumbers = [1, 2, 3, 4];
  const timeslotById = new Map<number, any>(
    (data.timeslots || []).filter((ts: any) => !!ts?.id).map((ts: any) => [ts.id, ts])
  );

  const getEffectivePeriodicity = (lesson: any) => {
    const timeslot = timeslotById.get(lesson.timeslotId);
    return timeslot?.weekParity && timeslot.weekParity !== 'WEEKLY'
      ? timeslot.weekParity
      : lesson.periodicity;
  };

  const handleDragStart = (e: React.DragEvent, lessonId: number) => {
    if (published || baseTemplateLocked) {
      e.preventDefault();
      if (baseTemplateLocked) {
        Notification.show(BASE_TEMPLATE_LOCKED_MESSAGE, { theme: 'primary', position: 'bottom-end' });
      }
      return;
    }
    e.dataTransfer.setData('lessonId', lessonId.toString());
    e.dataTransfer.effectAllowed = 'move';
  };

  const handleDragOver = (e: React.DragEvent) => {
    if (published || baseTemplateLocked) return;
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
  };

  const handleDrop = async (e: React.DragEvent, day: string, lessonNum: number, groupId: number, periodicity = 'WEEKLY') => {
    if (published || baseTemplateLocked) {
      if (baseTemplateLocked) {
        Notification.show(BASE_TEMPLATE_LOCKED_MESSAGE, { theme: 'primary', position: 'bottom-end' });
      }
      return;
    }
    e.preventDefault();
    e.stopPropagation();
    const lessonIdStr = e.dataTransfer.getData('lessonId');
    if (!lessonIdStr) return;
    
    const lessonId = parseInt(lessonIdStr, 10);
    const targetTimeslot = data.timeslots?.find((ts: any) => 
      ts?.dayOfWeek === day && 
      ts?.lessonNumber === lessonNum &&
      (ts?.weekParity === periodicity || ts?.weekParity === 'WEEKLY')
    );
    if (!targetTimeslot) return;

    try {
      await (ScheduleEndpoint as any).moveLesson(lessonId as any, targetTimeslot.id as any, "", periodicity as any);
      await refreshSchedule();
    } catch (err) {
      console.error('Failed to move lesson:', err);
      Notification.show(getMutationErrorMessage(err, 'Помилка під час перенесення заняття'), { theme: 'error' });
    }
  };

  const handleCellClick = (day: string, lessonNum: number, groupId: number) => {
    if (published) return;
    if (baseTemplateLocked) {
      Notification.show(BASE_TEMPLATE_LOCKED_MESSAGE, { theme: 'primary', position: 'bottom-end' });
      return;
    }
    const targetTimeslot = data.timeslots?.find((ts: any) => ts?.dayOfWeek === day && ts?.lessonNumber === lessonNum);
    
    setAssignDialogState({
      opened: true,
      day,
      lessonNum,
      groupId,
      timeslotId: targetTimeslot?.id
    });
  };

  return (
    <div className="overflow-auto max-w-full h-full bg-white p-4 font-serif">
      <div className="mb-3 flex flex-wrap items-center justify-between gap-3 border border-gray-200 bg-white px-3 py-2 shadow-sm">
        <div className="flex flex-wrap items-center gap-2">
          <span className="text-xs font-semibold uppercase tracking-wide text-gray-500">Курс:</span>
          {COURSE_FILTERS.map((course) => {
            const selected = effectiveCourse === course;
            const count = courseCounts.get(course) || 0;

            return (
              <button
                key={course}
                type="button"
                disabled={count === 0}
                onClick={() => {
                  selectedCourseFilter.value = course;
                }}
                className={`h-9 min-w-[88px] border px-3 text-sm font-semibold transition-colors ${
                  selected
                    ? 'border-gray-900 bg-gray-900 text-white'
                    : 'border-gray-300 bg-white text-gray-700 hover:border-gray-500 hover:bg-gray-50'
                } ${count === 0 ? 'cursor-not-allowed opacity-40 hover:border-gray-300 hover:bg-white' : ''}`}
              >
                {course} курс
                <span className={`ml-2 text-xs ${selected ? 'text-gray-200' : 'text-gray-500'}`}>{count}</span>
              </button>
            );
          })}
          <button
            type="button"
            onClick={() => {
              selectedCourseFilter.value = 'ALL';
            }}
            className={`h-9 min-w-[76px] border px-3 text-sm font-semibold transition-colors ${
              effectiveCourse === 'ALL'
                ? 'border-gray-900 bg-gray-900 text-white'
                : 'border-gray-300 bg-white text-gray-700 hover:border-gray-500 hover:bg-gray-50'
            }`}
          >
            Усі
          </button>
        </div>
        <div className="text-xs font-medium text-gray-500">
          Показано: {groups.length} з {allGroups.length}
        </div>
      </div>
      <table className="border-collapse w-full border-[2.5px] border-black text-black bg-white">
        <thead>
          <tr>
            <th colSpan={2} rowSpan={2} className="border-r-[2.5px] border-b-[2.5px] border-black bg-white w-12"></th>
            {groups.map((group) => (
              <th 
                key={`group-${group.id}`} 
                className="border-r border-black border-b-[1px] p-2 font-sans font-bold text-xl uppercase whitespace-nowrap text-center align-middle bg-white min-w-[140px]"
              >
                {group.name}
              </th>
            ))}
          </tr>
          <tr>
            {groups.map((group) => (
              <th 
                key={`curator-${group.id}`} 
                className="border-r border-black border-b-[2.5px] p-1 text-[13px] font-normal font-serif text-center align-middle whitespace-nowrap"
              >
                {group.curatorName || '—'}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {days.map((day) => (
            <React.Fragment key={day}>
              {lessonNumbers.map((num, idx) => (
                <tr key={`${day}-${num}`}>
                  {idx === 0 && (
                    <td 
                      rowSpan={lessonNumbers.length} 
                      className="border-r-[2.5px] border-b-[2.5px] border-black p-0 text-center align-middle bg-white w-12"
                    >
                      <div className="flex items-center justify-center h-full w-full" style={{ writingMode: 'vertical-rl', transform: 'rotate(180deg)' }}>
                        <span className="text-[18px] font-black lowercase py-4 leading-none tracking-tight">{dayNames[day]}</span>
                      </div>
                    </td>
                  )}

                  <td className="border-[2.5px] border-black p-1 text-center align-middle font-black text-3xl w-10">
                    {num}
                  </td>
                  {groups.map((group) => {
                    const slotLessons = data.lessons?.filter((l: any) => 
                      l.groupName === group.name && 
                      data.timeslots?.find((ts: any) => ts.id === l.timeslotId)?.dayOfWeek === day &&
                      data.timeslots?.find((ts: any) => ts.id === l.timeslotId)?.lessonNumber === num
                    ) || [];

                    const groupedBySubject: Array<[string, any[]]> = Array.from(
                      slotLessons.reduce((map: Map<string, any[]>, lesson: any) => {
                        const key = lesson.subjectName || 'Невідома дисципліна';
                        if (!map.has(key)) map.set(key, []);
                        map.get(key)!.push(lesson);
                        return map;
                      }, new Map<string, any[]>())
                    );

                    let numeratorLessons = slotLessons.filter((l: any) => getEffectivePeriodicity(l) === 'ODD_WEEKS');
                    let denominatorLessons = slotLessons.filter((l: any) => getEffectivePeriodicity(l) === 'EVEN_WEEKS');
                    const weeklyLessons = slotLessons.filter((l: any) => getEffectivePeriodicity(l) === 'WEEKLY');

                    // If old/generated data already contains two lessons in one physical cell without
                    // ODD/EVEN metadata, render it as a numerator/denominator cell instead of a stack.
                    const fallbackSplit = numeratorLessons.length === 0 && denominatorLessons.length === 0 && weeklyLessons.length > 1;
                    if (fallbackSplit) {
                      numeratorLessons = [weeklyLessons[0]];
                      denominatorLessons = weeklyLessons.slice(1);
                    }

                    const hasNumerator = numeratorLessons.length > 0;
                    const hasDenominator = denominatorLessons.length > 0;
                    const hasWeekly = slotLessons.some((l: any) => getEffectivePeriodicity(l) === 'WEEKLY');
                    const hasSplitSubgroups = slotLessons.length > 1 && slotLessons.some((l: any) => l.subgroup > 0);
                    
                    const isDiagonal = !hasSplitSubgroups && (fallbackSplit || (!hasWeekly && (hasNumerator || hasDenominator)));
                    const isFull = hasSplitSubgroups || (!fallbackSplit && (hasWeekly || (hasNumerator && hasDenominator)));

                    return (
                      <td 
                        key={`${day}-${num}-${group.id}`} 
                        className={`border-r p-0 align-top h-[90px] relative border-black ${idx === lessonNumbers.length - 1 ? 'border-b-[2.5px]' : 'border-b'} ${isFull ? 'cursor-default' : 'cursor-pointer'}`}
                        style={isDiagonal ? {
                          background: 'linear-gradient(to bottom right, white calc(50% - 1px), black 50%, white calc(50% + 1px))'
                        } : {}}
                        onDragOver={handleDragOver}
                        onDrop={(e) => handleDrop(e, day, num, group.id!, 'WEEKLY')}
                      >
                        <div className="relative w-full h-full">
                          {slotLessons.length === 0 ? (
                            <div 
                              className="h-full w-full opacity-0 hover:opacity-5 bg-blue-500 cursor-pointer"
                              onClick={() => handleCellClick(day, num, group.id!)}
                            ></div>
                          ) : isDiagonal ? (
                            <>
                              {/* Numerator Area (Odd) */}
                              <div 
                                className={`absolute top-0 left-0 w-1/2 h-1/2 flex items-start justify-start p-1 z-10 ${!hasNumerator ? 'hover:bg-blue-50/50 cursor-pointer' : ''}`}
                                onDragOver={handleDragOver}
                                onDrop={(e) => handleDrop(e, day, num, group.id!, 'ODD_WEEKS')}
                                onClick={() => !hasNumerator && handleCellClick(day, num, group.id!)}
                              >
                                {hasNumerator && (
                                  <div className="w-[140%] text-left">
                                    <GridCell 
                                      lessons={numeratorLessons}
                                      mode="GROUP"
                                      compact={true}
                                      align="left"
                                      suppressConflictIndicator={fallbackSplit}
                                      onDragStart={handleDragStart}
                                    />
                                  </div>
                                )}
                              </div>

                              {/* Denominator Area (Even) */}
                              <div 
                                className={`absolute bottom-0 right-0 w-1/2 h-1/2 flex items-end justify-end p-1 z-10 ${!hasDenominator ? 'hover:bg-blue-50/50 cursor-pointer' : ''}`}
                                onDragOver={handleDragOver}
                                onDrop={(e) => handleDrop(e, day, num, group.id!, 'EVEN_WEEKS')}
                                onClick={() => !hasDenominator && handleCellClick(day, num, group.id!)}
                              >
                                {hasDenominator && (
                                  <div className="w-[140%] text-right">
                                    <GridCell 
                                      lessons={denominatorLessons}
                                      mode="GROUP"
                                      compact={true}
                                      align="right"
                                      suppressConflictIndicator={fallbackSplit}
                                      onDragStart={handleDragStart}
                                    />
                                  </div>
                                )}
                              </div>
                            </>
                          ) : (
                            <div 
                              className="flex flex-col gap-0 w-full p-1 justify-start h-full items-center"
                              onClick={() => !hasWeekly && handleCellClick(day, num, group.id!)}
                            >
                              {groupedBySubject.map(([subject, groupLessons], groupIdx) => (
                                <React.Fragment key={subject}>
                                  {groupIdx > 0 && <div className="border-t border-black w-full my-0.5" />}
                                  <GridCell 
                                    lessons={groupLessons}
                                    mode="GROUP"
                                    onDragStart={handleDragStart}
                                    align="center"
                                  />
                                </React.Fragment>
                              ))}
                            </div>
                          )}
                        </div>
                      </td>
                    );
                  })}
                </tr>
              ))}
            </React.Fragment>
          ))}
        </tbody>
      </table>

      {assignDialogState.opened && (
        <AssignLessonDialog
          opened={assignDialogState.opened}
          day={assignDialogState.day}
          lessonNum={assignDialogState.lessonNum}
          groupId={assignDialogState.groupId}
          timeslotId={assignDialogState.timeslotId}
          onClose={() => setAssignDialogState({ ...assignDialogState, opened: false })}
        />
      )}
    </div>
  );
};
