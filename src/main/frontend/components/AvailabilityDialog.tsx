import React, { useEffect, useState } from 'react';
import { Dialog } from '@vaadin/react-components/Dialog.js';
import { Button } from '@vaadin/react-components/Button.js';
import { Icon } from '@vaadin/react-components/Icon.js';
import { HorizontalLayout } from '@vaadin/react-components/HorizontalLayout.js';
import { Notification } from '@vaadin/react-components/Notification.js';
import { ScheduleEndpoint, TeacherAvailabilityEndpoint } from '../generated/endpoints';
import type AvailabilityDTO from '../generated/com/sergofoox/domain/ui/dto/AvailabilityDTO';
import type LessonDTO from '../generated/com/sergofoox/domain/ui/dto/LessonDTO';
import AvailabilityStatus from '../generated/com/sergofoox/domain/teacher/AvailabilityStatus';
import type TeacherDTO from '../generated/com/sergofoox/domain/ui/dto/TeacherDTO';
import { BASE_TEMPLATE_LOCKED_MESSAGE, getMutationErrorMessage, isBaseTemplateLocked } from '../store/app-state';
import { notifyDataChanged } from '../utils/cross-tab-sync';

interface AvailabilityDialogProps {
  opened: boolean;
  teacher?: TeacherDTO;
  onClose: () => void;
}

const DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'];
const LESSON_NUMBERS = [1, 2, 3, 4];
const cellKey = (day: string, lesson: number) => `${day}-${lesson}`;

const dayLabels: Record<string, string> = {
  MONDAY: 'Пн',
  TUESDAY: 'Вт',
  WEDNESDAY: 'Ср',
  THURSDAY: 'Чт',
  FRIDAY: 'Пт',
};

export const AvailabilityDialog: React.FC<AvailabilityDialogProps> = ({ opened, teacher, onClose }) => {
  const [availabilities, setAvailabilities] = useState<AvailabilityDTO[]>([]);
  const [scheduledByCell, setScheduledByCell] = useState<Record<string, LessonDTO[]>>({});
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (opened && teacher?.id) {
      Promise.all([
        TeacherAvailabilityEndpoint.getAvailability(teacher.id as any),
        ScheduleEndpoint.getScheduleGridData()
      ])
        .then(([availabilityData, scheduleData]) => {
          setAvailabilities((availabilityData || []).filter(item => !!item) as AvailabilityDTO[]);

          const timeslotsById = new Map(
            (scheduleData?.timeslots || [])
              .filter(item => !!item && item.id)
              .map(item => [item!.id!, item!])
          );
          const scheduled: Record<string, LessonDTO[]> = {};
          (scheduleData?.lessons || [])
            .filter(item => !!item && item.teacherId === teacher.id && item.timeslotId)
            .forEach(item => {
              const lesson = item!;
              const timeslot = timeslotsById.get(lesson.timeslotId!);
              if (!timeslot?.lessonNumber || typeof timeslot.dayOfWeek !== 'string') return;
              const key = cellKey(timeslot.dayOfWeek, timeslot.lessonNumber);
              scheduled[key] = [...(scheduled[key] || []), lesson];
            });
          setScheduledByCell(scheduled);
        })
        .catch(err => console.error('Failed to load availability data:', err));
    } else if (!opened) {
      setAvailabilities([]);
      setScheduledByCell({});
    }
  }, [opened, teacher]);

  const getStatus = (day: string, lesson: number) => {
    return availabilities.find(a => a.dayOfWeek === day && a.lessonNumber === lesson)?.status;
  };

  const getScheduledLessons = (day: string, lesson: number) => scheduledByCell[cellKey(day, lesson)] || [];

  const toggleStatus = (day: string, lesson: number) => {
    const current = getStatus(day, lesson);
    let next: AvailabilityStatus | undefined;
    if (!current) next = AvailabilityStatus.PREFERRED;
    else if (current === AvailabilityStatus.PREFERRED) next = AvailabilityStatus.UNAVAILABLE;
    else next = undefined;

    const filtered = availabilities.filter(a => !(a.dayOfWeek === day && a.lessonNumber === lesson));
    if (next) {
      setAvailabilities([...filtered, { dayOfWeek: day as any, lessonNumber: lesson, status: next }]);
    } else {
      setAvailabilities(filtered);
    }
  };

  const handleSave = async () => {
    if (!teacher?.id) return;
    if (isBaseTemplateLocked.value) {
      Notification.show(BASE_TEMPLATE_LOCKED_MESSAGE, { theme: 'primary', position: 'bottom-end' });
      return;
    }
    setSaving(true);
    try {
      await TeacherAvailabilityEndpoint.saveAvailability(teacher.id as any, availabilities);
      Notification.show('Преференції збережено', { theme: 'success', position: 'bottom-end' });
      notifyDataChanged('availability');
      onClose();
    } catch (err) {
      console.error('Failed to save availability:', err);
      Notification.show(getMutationErrorMessage(err, 'Помилка збереження'), { theme: 'error', position: 'bottom-end' });
    } finally {
      setSaving(false);
    }
  };

  const renderCell = (day: string, lesson: number) => {
    const status = getStatus(day, lesson);
    const scheduledLessons = getScheduledLessons(day, lesson);
    const hasScheduledLesson = scheduledLessons.length > 0;
    let color = 'bg-white';
    let icon = null;
    if (status === AvailabilityStatus.PREFERRED) {
      color = 'bg-green-100 text-green-700';
      icon = <Icon icon="vaadin:star" className="w-4 h-4" />;
    } else if (status === AvailabilityStatus.UNAVAILABLE) {
      color = 'bg-red-100 text-red-700';
      icon = <Icon icon="vaadin:close" className="w-4 h-4" />;
    } else if (hasScheduledLesson) {
      color = 'bg-blue-50 text-blue-800';
    }

    const title = [
      `${dayLabels[day]}, пара ${lesson}`,
      ...scheduledLessons.map(item => `${item.subjectName || 'Заняття'}${item.groupName ? `, ${item.groupName}` : ''}${item.roomName ? `, ${item.roomName}` : ''}`)
    ].join('\n');

    return (
      <div
        key={cellKey(day, lesson)}
        onClick={() => toggleStatus(day, lesson)}
        className={`border p-1 flex items-center justify-center cursor-pointer transition-colors hover:bg-gray-50 min-h-14 ${hasScheduledLesson ? 'ring-1 ring-inset ring-blue-300' : ''} ${color}`}
        title={title}
      >
        <div className="min-w-0 max-w-full flex flex-col items-center justify-center gap-0.5 text-center leading-tight">
          {icon}
          {hasScheduledLesson && (
            <>
              <span className="max-w-full truncate text-[11px] font-semibold">
                {scheduledLessons.length > 1 ? `${scheduledLessons.length} заняття` : scheduledLessons[0].subjectName || 'Заняття'}
              </span>
              {scheduledLessons.length === 1 && scheduledLessons[0].groupName && (
                <span className="max-w-full truncate text-[10px] text-blue-700">
                  {scheduledLessons[0].groupName}
                </span>
              )}
            </>
          )}
        </div>
      </div>
    );
  };

  return (
    <Dialog
      headerTitle={`Графік доступності: ${teacher?.fullName}`}
      opened={opened}
      onOpenedChanged={(e) => !e.detail.value && onClose()}
      footerRenderer={() => (
        <HorizontalLayout theme="spacing">
          <Button theme="primary" onClick={handleSave} disabled={saving}>
            {saving ? 'Збереження...' : 'Зберегти'}
          </Button>
          <Button onClick={onClose}>Закрити</Button>
        </HorizontalLayout>
      )}
    >
      <div className="flex flex-col gap-4 w-[min(92vw,560px)]">
        <div className="flex flex-wrap gap-4 text-xs font-medium text-gray-500 mb-2 p-2 bg-gray-50 rounded-lg">
            <div className="flex items-center gap-1"><div className="w-3 h-3 bg-green-100 border border-green-200"></div> Бажано</div>
            <div className="flex items-center gap-1"><div className="w-3 h-3 bg-red-100 border border-red-200"></div> Неможливо</div>
            <div className="flex items-center gap-1"><div className="w-3 h-3 bg-white border border-gray-200"></div> Нейтрально</div>
            <div className="flex items-center gap-1"><div className="w-3 h-3 bg-blue-50 border border-blue-300"></div> Зайнято</div>
        </div>
        <div
          className="grid border rounded-lg overflow-hidden shadow-sm"
          style={{ gridTemplateColumns: `repeat(${DAYS.length + 1}, minmax(0, 1fr))` }}
        >
          <div className="border bg-gray-100 p-2 font-bold text-center text-gray-600 flex items-center justify-center">#</div>
          {DAYS.map(day => (
            <div key={day} className="border bg-gray-100 p-2 font-bold text-center text-xs text-gray-600 flex items-center justify-center">
              {dayLabels[day]}
            </div>
          ))}
          {LESSON_NUMBERS.map(lesson => (
            <React.Fragment key={lesson}>
              <div className="border bg-gray-50 p-2 font-bold text-center text-gray-500 flex items-center justify-center">{lesson}</div>
              {DAYS.map(day => renderCell(day, lesson))}
            </React.Fragment>
          ))}
        </div>
      </div>
    </Dialog>
  );
};
