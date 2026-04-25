import React, { useEffect, useState } from 'react';
import { GridCell } from './GridCell';
import { scheduleData, scheduleLoading, refreshSchedule, isPublished } from '../store/app-state';
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

export const ScheduleGrid: React.FC = () => {
  const data = scheduleData.value;
  const loading = scheduleLoading.value;
  const published = isPublished.value;

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

  if (loading) return <div className="p-8 text-center text-gray-500 animate-pulse font-serif">Завантаження матриці розкладу...</div>;
  if (!data) return <div className="p-8 text-red-500 text-center font-bold font-serif">Помилка: дані розкладу не завантажено</div>;

  const days = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'];
  const lessonNumbers = [1, 2, 3, 4];
  const groups = [...(data.groups || [])].filter(g => !!g).sort((a, b) => a.name.localeCompare(b.name));

  const handleDragStart = (e: React.DragEvent, lessonId: number) => {
    if (published) {
      e.preventDefault();
      return;
    }
    e.dataTransfer.setData('lessonId', lessonId.toString());
    e.dataTransfer.effectAllowed = 'move';
  };

  const handleDragOver = (e: React.DragEvent) => {
    if (published) return;
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
  };

  const handleDrop = async (e: React.DragEvent, day: string, lessonNum: number, groupId: number) => {
    if (published) return;
    e.preventDefault();
    const lessonIdStr = e.dataTransfer.getData('lessonId');
    if (!lessonIdStr) return;
    
    const lessonId = parseInt(lessonIdStr, 10);
    const targetTimeslot = data.timeslots?.find((ts: any) => ts?.dayOfWeek === day && ts?.lessonNumber === lessonNum);
    if (!targetTimeslot) return;

    try {
      await (ScheduleEndpoint as any).moveLesson(lessonId as any, targetTimeslot.id as any, "", groupId as any);
      await refreshSchedule();
    } catch (err) {
      console.error('Failed to move lesson:', err);
    }
  };

  const handleCellClick = (day: string, lessonNum: number, groupId: number) => {
    if (published) return;
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
                        const key = lesson.subjectName || 'Невідомий предмет';
                        if (!map.has(key)) map.set(key, []);
                        map.get(key)!.push(lesson);
                        return map;
                      }, new Map<string, any[]>())
                    );

                    const hasNumerator = slotLessons.some((l: any) => l.periodicity === 'ODD_WEEKS');
                    const hasDenominator = slotLessons.some((l: any) => l.periodicity === 'EVEN_WEEKS');
                    const hasWeekly = slotLessons.some((l: any) => l.periodicity === 'WEEKLY');
                    
                    const isDiagonal = !hasWeekly && (hasNumerator || hasDenominator);
                    const isFull = hasWeekly || (hasNumerator && hasDenominator);

                    return (
                      <td 
                        key={`${day}-${num}-${group.id}`} 
                        className={`border-r p-0 align-top h-[90px] relative border-black ${idx === lessonNumbers.length - 1 ? 'border-b-[2.5px]' : 'border-b'} ${isFull ? 'cursor-default' : 'cursor-pointer'}`}
                        style={isDiagonal ? {
                          background: 'linear-gradient(to bottom right, white calc(50% - 1px), black 50%, white calc(50% + 1px))'
                        } : {}}
                        onDragOver={handleDragOver}
                        onDrop={(e) => handleDrop(e, day, num, group.id!)}
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
                                onClick={() => !hasNumerator && handleCellClick(day, num, group.id!)}
                              >
                                {hasNumerator && (
                                  <div className="w-[140%] text-left">
                                    <GridCell 
                                      lessons={slotLessons.filter((l: any) => l.periodicity === 'ODD_WEEKS')}
                                      mode="GROUP"
                                      compact={true}
                                      align="left"
                                      onDragStart={handleDragStart}
                                    />
                                  </div>
                                )}
                              </div>

                              {/* Denominator Area (Even) */}
                              <div 
                                className={`absolute bottom-0 right-0 w-1/2 h-1/2 flex items-end justify-end p-1 z-10 ${!hasDenominator ? 'hover:bg-blue-50/50 cursor-pointer' : ''}`}
                                onClick={() => !hasDenominator && handleCellClick(day, num, group.id!)}
                              >
                                {hasDenominator && (
                                  <div className="w-[140%] text-right">
                                    <GridCell 
                                      lessons={slotLessons.filter((l: any) => l.periodicity === 'EVEN_WEEKS')}
                                      mode="GROUP"
                                      compact={true}
                                      align="right"
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
