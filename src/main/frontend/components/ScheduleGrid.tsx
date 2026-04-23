import React, { useEffect, useState } from 'react';
import { GridCell } from './GridCell';
import { scheduleData, scheduleLoading, refreshSchedule, isPublished } from '../store/app-state';
import { ScheduleEndpoint } from '../generated/endpoints';
import { AssignLessonDialog } from './AssignLessonDialog';

const dayNames: Record<string, string> = {
  MONDAY: 'Понеділок',
  TUESDAY: 'Вівторок',
  WEDNESDAY: 'Середа',
  THURSDAY: 'Четвер',
  FRIDAY: 'П\'ятниця',
  SATURDAY: 'Субота',
  SUNDAY: 'Неділя'
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
      <table className="border-collapse table-fixed w-full min-w-[1000px] border-2 border-black text-black">
        <thead className="bg-white">
          <tr>
            <th rowSpan={2} className="w-12 border-r-2 border-b-2 border-black p-1 text-base font-black uppercase text-center align-middle">
              День
            </th>
            <th rowSpan={2} className="w-8 border-r-2 border-b-2 border-black p-1 text-lg font-black uppercase text-center align-middle">
              №
            </th>
            {groups.map((group) => (
              <th key={`group-${group.id}`} className="border-r-2 border-b border-black p-1 font-black text-center text-2xl uppercase tracking-tighter bg-gray-50/20">
                {group.name}
              </th>
            ))}
          </tr>
          <tr>
            {groups.map((group) => (
              <th key={`curator-${group.id}`} className="border-r-2 border-b-2 border-black p-0.5 text-[12px] text-center font-normal">
                {group.curatorName || '—'}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {days.map((day) => (
            <React.Fragment key={day}>
              {lessonNumbers.map((num, idx) => (
                <tr key={`${day}-${num}`} className="hover:bg-gray-50/10 transition-colors">
                  {idx === 0 && (
                    <td 
                      rowSpan={lessonNumbers.length} 
                      className="border-r-2 border-b-2 border-black p-0.5 font-bold text-center align-middle"
                    >
                      <div className="flex items-center justify-center h-full w-full" style={{ writingMode: 'vertical-rl', transform: 'rotate(180deg)' }}>
                        <span className="text-xs uppercase tracking-widest font-black">{dayNames[day]}</span>
                      </div>
                    </td>
                  )}
                  <td className="border-r-2 border-b-2 border-black p-0.5 text-center font-black text-xl">
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
                        className="border-r-2 border-b-2 border-black p-0.5 align-top h-[80px] relative cursor-pointer"
                        onDragOver={handleDragOver}
                        onDrop={(e) => handleDrop(e, day, num)}
                        onClick={(e) => {
                           if (slotLessons.length === 0) {
                             handleCellClick(day, num, group.id!);
                           }
                        }}
                      >
                        <div className="flex flex-col h-full w-full">
                          {slotLessons.length === 0 ? (
                            <div className="h-full w-full opacity-0 hover:opacity-10 transition-opacity bg-blue-500"></div>
                          ) : (
                            <div className={`h-full grid ${slotLessons.length > 1 ? 'grid-rows-2' : 'grid-rows-1'} gap-[2px]`}>
                              {slotLessons.map((lesson: any, i: number) => (
                                <React.Fragment key={lesson.id}>
                                  <GridCell 
                                    lesson={lesson}
                                    mode="GROUP"
                                    onDragStart={handleDragStart}
                                  />
                                  {slotLessons.length > 1 && i === 0 && (
                                    <div className="border-b border-black w-full" />
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
