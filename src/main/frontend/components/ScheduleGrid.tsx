import React, { useEffect } from 'react';
import { ScheduleEndpoint } from '../generated/endpoints';
import type ScheduleGridDTO from '../generated/com/sergofoox/domain/ui/dto/ScheduleGridDTO';
import { GridCell } from './GridCell';
import { useSignal } from '@vaadin/hilla-react-signals';
import { selectedEntity, scheduleData, scheduleLoading, refreshSchedule, isPublished } from '../store/app-state';

import TimeslotDTO from '../generated/com/sergofoox/domain/ui/dto/TimeslotDTO';
import LessonDTO from '../generated/com/sergofoox/domain/ui/dto/LessonDTO';

interface ScheduleGridProps {
  mode: 'GROUP' | 'TEACHER' | 'ROOM';
}

const dayNames: Record<string, string> = {
  MONDAY: 'Пн',
  TUESDAY: 'Вт',
  WEDNESDAY: 'Ср',
  THURSDAY: 'Чт',
  FRIDAY: 'Пт',
  SATURDAY: 'Сб',
  SUNDAY: 'Нд'
};

export const ScheduleGrid: React.FC<ScheduleGridProps> = ({ mode }) => {
  // Use global signals directly. Hilla React Signals will automatically track dependencies.
  const data = scheduleData.value;
  const loading = scheduleLoading.value;
  const published = isPublished.value;

  useEffect(() => {
    refreshSchedule();
  }, []);

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

  const handleDrop = async (e: React.DragEvent, timeslotId: number, targetEntity: any) => {
    if (published) return;
    e.preventDefault();
    const lessonIdStr = e.dataTransfer.getData('lessonId');
    if (!lessonIdStr) return;
    
    const lessonId = parseInt(lessonIdStr, 10);
    const lesson = data?.lessons?.find((l) => l?.id === lessonId);
    if (!lesson) return;

    let targetRoomName = lesson.roomName;
    if (mode === 'ROOM') {
      targetRoomName = targetEntity.name;
    }

    try {
      await ScheduleEndpoint.moveLesson(lessonId as any, timeslotId as any, targetRoomName as any);
      await refreshSchedule();
    } catch (err) {
      console.error('Failed to move lesson:', err);
      alert('Помилка при переміщенні заняття');
    }
  };

  const selectEntity = (id: number) => {
    selectedEntity.value = { id, type: mode };
  };

  if (loading) return <div className="p-4 text-center">Завантаження розкладу...</div>;
  if (!data) return <div className="p-4 text-red-500 text-center">Помилка завантаження даних.</div>;

  const entities = mode === 'GROUP' ? data.groups : mode === 'TEACHER' ? data.teachers : data.rooms;
  const timeslots = data.timeslots || [];

  return (
    <div className="overflow-auto max-w-full border rounded-md shadow-md" style={{ maxHeight: 'calc(100vh - 200px)' }}>
      <table className="w-full border-collapse bg-[var(--aura-surface-color)]">
        <thead className="sticky top-0 z-20" style={{ backgroundColor: 'var(--aura-surface-color)' }}>
          <tr>
            <th className="sticky left-0 z-30 p-4 border text-left text-xs uppercase tracking-wider font-semibold" style={{ backgroundColor: 'var(--aura-surface-color)' }}>
              Час
            </th>
            {entities?.filter((e: any) => !!e).map((e: any) => (
              <th 
                key={e.id} 
                className={`p-4 border min-w-150 text-left text-s font-bold cursor-pointer hover:bg-blue-50 transition-colors ${selectedEntity.value?.id === e.id ? 'bg-blue-50 border-b-2 border-b-primary' : ''}`}
                onClick={() => selectEntity(e.id)}
              >
                {mode === 'TEACHER' ? e.fullName : e.name}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {timeslots.filter(t => !!t).map((ts) => (
            <tr key={ts!.id} className="hover:bg-gray-50 transition-colors">
              <td className="sticky left-0 z-10 p-4 border text-xs whitespace-nowrap" style={{ backgroundColor: 'var(--aura-surface-color)' }}>
                <div className="font-bold text-blue-600">{String(dayNames[ts!.dayOfWeek as string] || ts!.dayOfWeek || '')}</div>
                <div className="text-gray-500">{ts!.startTime?.substring(0, 5)} - {ts!.endTime?.substring(0, 5)}</div>
                {ts!.weekParity !== 'WEEKLY' && (
                  <div className="mt-1 text-[10px] py-1px px-1 rounded bg-gray-100 inline-block font-medium">
                    {ts!.weekParity === 'ODD_WEEKS' ? 'Чисельник' : 'Знаменник'}
                  </div>
                )}
              </td>
              {entities?.filter((e: any) => !!e).map((e: any) => {
                const entityName = mode === 'TEACHER' ? e.fullName : e.name;
                const lesson = data.lessons?.find((l) => 
                  l?.timeslotId === ts!.id && (
                    (mode === 'GROUP' && l?.groupName === entityName) ||
                    (mode === 'TEACHER' && l?.teacherName === entityName) ||
                    (mode === 'ROOM' && l?.roomName === entityName)
                  )
                );
                return (
                  <td key={e.id} className={`p-1 border align-top h-full ${selectedEntity.value?.id === e.id ? 'bg-blue-50/50' : ''}`}>
                    <GridCell 
                      lesson={lesson || undefined} 
                      mode={mode} 
                      onDragStart={handleDragStart}
                      onDragOver={handleDragOver}
                      onDrop={(event) => handleDrop(event, ts!.id as any, e)}
                    />
                  </td>
                );
              })}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};
