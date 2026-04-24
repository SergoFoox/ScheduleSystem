import React, { useEffect, useState } from 'react';
import { GridCell } from './GridCell';
import { scheduleData, scheduleLoading, refreshSchedule, isPublished } from '../store/app-state';
import { ScheduleEndpoint } from '../generated/endpoints';
import { AssignLessonDialog } from './AssignLessonDialog';

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

  if (loading) return <div className="p-8 text-center text-gray-500 animate-pulse">Завантаження матриці розкладу...</div>;
  if (!data) return <div className="p-8 text-red-500 text-center font-bold">Помилка: дані розкладу не завантажено</div>;

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

  const handleDrop = async (e: React.DragEvent, day: string, lessonNum: number) => {
    if (published) return;
    e.preventDefault();
    const lessonIdStr = e.dataTransfer.getData('lessonId');
    if (!lessonIdStr) return;
    
    const lessonId = parseInt(lessonIdStr, 10);
    const targetTimeslot = data.timeslots?.find((ts: any) => ts?.dayOfWeek === day && ts?.lessonNumber === lessonNum);
    if (!targetTimeslot) return;

    try {
      await ScheduleEndpoint.moveLesson(lessonId as any, targetTimeslot.id as any, "");
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
    <div className="overflow-auto max-w-full h-full bg-white p-4" style={{ fontFamily: '"Times New Roman", Times, serif' }}>
      <table className="border-collapse w-full border-[2.5px] border-black text-black bg-white">
        <thead>
          <tr>
            {/* Об'єднаний нерозділений квадрат у лівому куті */}
            <th colSpan={2} rowSpan={2} className="border-[2.5px] border-black bg-white"></th>
            {groups.map((group) => (
              <th 
                key={`group-${group.id}`} 
                className="border-[2.5px] border-black p-2 font-sans font-bold text-xl uppercase whitespace-nowrap text-center align-middle bg-white min-w-[140px]"
              >
                {group.name}
              </th>
            ))}
          </tr>
          <tr>
            {groups.map((group) => (
              <th 
                key={`curator-${group.id}`} 
                className="border-[2.5px] border-black p-1 text-[13px] font-normal text-center align-middle whitespace-nowrap"
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
                      className="border-[2.5px] border-black p-0 text-center align-middle bg-white w-12"
                    >
                      <div className="flex items-center justify-center h-full w-full" style={{ writingMode: 'vertical-rl', transform: 'rotate(180deg)' }}>
                        <span className="text-[18px] font-bold lowercase py-2">{dayNames[day]}</span>
                      </div>
                    </td>
                  )}

                  <td className="border-[2.5px] border-black p-1 text-center align-middle font-bold text-2xl w-10">
                    {num}
                  </td>
                  {groups.map((group) => {
                    const slotLessons = data.lessons?.filter((l: any) => 
                      l.groupName === group.name && 
                      data.timeslots?.find((ts: any) => ts.id === l.timeslotId)?.dayOfWeek === day &&
                      data.timeslots?.find((ts: any) => ts.id === l.timeslotId)?.lessonNumber === num
                    ) || [];

                    return (
                      <td 
                        key={`${day}-${num}-${group.id}`} 
                        className="border-[2.5px] border-black p-1 align-middle h-[100px] relative cursor-pointer hover:bg-gray-50"
                        onDragOver={handleDragOver}
                        onDrop={(e) => handleDrop(e, day, num)}
                        onClick={(e) => {
                           if (slotLessons.length === 0) {
                             handleCellClick(day, num, group.id!);
                           }
                        }}
                      >
                        <div className="flex flex-col h-full w-full justify-center">
                          {slotLessons.length === 0 ? (
                            <div className="h-full w-full opacity-0 hover:opacity-5 bg-blue-500"></div>
                          ) : (
                            <div className={`h-full grid ${slotLessons.length > 1 ? 'grid-rows-2' : 'grid-rows-1'} gap-0`}>
                              {slotLessons.map((lesson: any, i: number) => (
                                <React.Fragment key={lesson.id}>
                                  <GridCell 
                                    lesson={lesson}
                                    mode="GROUP"
                                    onDragStart={handleDragStart}
                                  />
                                  {slotLessons.length > 1 && i === 0 && (
                                    <div className="border-b-[2.5px] border-black w-full" />
                                  )}
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
