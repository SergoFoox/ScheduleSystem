import React, { useState, useEffect } from 'react';
import { Dialog } from '@vaadin/react-components/Dialog.js';
import { Button } from '@vaadin/react-components/Button.js';
import { Select } from '@vaadin/react-components/Select.js';
import { Notification } from '@vaadin/react-components/Notification.js';
import { ScheduleEndpoint, RoomEndpoint, CoursePlanEndpoint } from '../generated/endpoints';
import { getMutationErrorMessage, refreshSchedule } from '../store/app-state';
import type ReplacementCandidateDTO from '../generated/com/sergofoox/domain/ui/dto/ReplacementCandidateDTO';
import type RoomDTO from '../generated/com/sergofoox/domain/ui/dto/RoomDTO';
import type TeacherDTO from '../generated/com/sergofoox/domain/ui/dto/TeacherDTO';
import type CoursePlanDTO from '../generated/com/sergofoox/domain/ui/dto/CoursePlanDTO';
import { formatPriority, formatRoomType } from '../utils/labels';

interface ReplacementDialogProps {
  lesson: any;
  opened: boolean;
  onClose: () => void;
}

export const ReplacementDialog: React.FC<ReplacementDialogProps> = ({ lesson, opened, onClose }) => {
  const [candidates, setCandidates] = useState<ReplacementCandidateDTO[]>([]);
  const [rooms, setRooms] = useState<RoomDTO[]>([]);
  const [plans, setPlans] = useState<CoursePlanDTO[]>([]);
  
  const [selectedSubjectId, setSelectedSubjectId] = useState<string | undefined>(lesson.subjectId?.toString());
  const [selectedTeacherId, setSelectedTeacherId] = useState<string | undefined>(lesson.teacherId?.toString());
  const [selectedRoomId, setSelectedRoomId] = useState<string | undefined>(lesson.roomId?.toString());
  
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (opened && lesson.id) {
      setLoading(true);
      Promise.all([
        ScheduleEndpoint.getReplacementCandidates(lesson.id as any),
        RoomEndpoint.getAllRooms(),
        CoursePlanEndpoint.getPlansByGroup(lesson.groupId as any)
      ]).then(([candidateData, roomData, planData]) => {
        setCandidates(candidateData || []);
        setRooms((roomData || []).filter(r => !!r) as RoomDTO[]);
        setPlans((planData || []).filter(p => !!p) as CoursePlanDTO[]);
      }).catch(err => {
        console.error('Failed to load data:', err);
      }).finally(() => {
        setLoading(false);
      });
    }
  }, [opened, lesson.id, lesson.groupId]);

  // Re-load candidates if subject changes
  useEffect(() => {
      if (opened && selectedSubjectId && selectedSubjectId !== lesson.subjectId?.toString()) {
          // In a real scenario, we might need a more specific endpoint to get candidates by subject
          // For now, we'll keep current candidates or re-fetch for the lesson
          ScheduleEndpoint.getScheduleGridData().then(data => {
              // This is a simplified fallback to show all teachers if subject changed
              // Ideally we'd filter by competence matrix here
          });
      }
  }, [selectedSubjectId, opened]);

  const handleApplyChanges = async () => {
    if (!lesson.id) return;
    setSaving(true);
    try {
      await (ScheduleEndpoint as any).assignReplacement(
        lesson.id as any,
        selectedTeacherId ? parseInt(selectedTeacherId) as any : undefined,
        selectedRoomId ? parseInt(selectedRoomId) as any : undefined,
        selectedSubjectId ? parseInt(selectedSubjectId) as any : undefined
      );
      Notification.show('Зміни збережено', { theme: 'success' });
      await refreshSchedule();
      onClose();
    } catch (err) {
      console.error('Failed to assign replacement:', err);
      Notification.show(getMutationErrorMessage(err, 'Помилка під час збереження змін'), { theme: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteLesson = async () => {
    if (!lesson.id) return;
    if (!confirm('Ви впевнені, що хочете видалити це заняття з розкладу?')) return;
    
    setSaving(true);
    try {
      await ScheduleEndpoint.unassignLesson(lesson.id as any);
      Notification.show('Заняття видалено', { theme: 'success' });
      await refreshSchedule();
      onClose();
    } catch (err) {
      console.error('Failed to unassign lesson:', err);
      Notification.show(getMutationErrorMessage(err, 'Помилка під час видалення заняття'), { theme: 'error' });
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog
      headerTitle={`Коригування заняття: ${lesson.groupName}`}
      opened={opened}
      onOpenedChanged={(e) => !e.detail.value && onClose()}
      footerRenderer={() => (
        <div className="flex justify-between w-full p-2">
          <Button theme="error" onClick={handleDeleteLesson} disabled={saving}>Видалити</Button>
          <div className="flex gap-2">
            <Button theme="primary" onClick={handleApplyChanges} disabled={saving || loading}>Застосувати</Button>
            <Button onClick={onClose}>Скасувати</Button>
          </div>
        </div>
      )}
    >
      <div className="p-4 flex flex-col gap-4 min-w-[450px]">
        {loading ? (
          <p>Завантаження кандидатів...</p>
        ) : (
          <>
            <Select
              label="Дисципліна"
              items={plans.map(p => ({
                label: p.subjectName || '—',
                value: (p.subjectId || '').toString()
              }))}
              value={selectedSubjectId}
              onValueChanged={(e) => setSelectedSubjectId(e.detail.value)}
              className="w-full"
            />

            <Select
              label="Викладач (кандидати на заміну)"
              items={candidates.map(c => ({
                label: `${c.fullName} (${formatPriority(c.priority)}, навантаження: ${c.currentWorkload})`,
                value: (c.id || '').toString()
              }))}
              value={selectedTeacherId}
              onValueChanged={(e) => setSelectedTeacherId(e.detail.value)}
              className="w-full"
            />

            <Select
              label="Аудиторія"
              items={rooms.map(r => ({
                label: `${r.name} (${formatRoomType(r.type)})`,
                value: (r.id || '').toString()
              }))}
              value={selectedRoomId}
              onValueChanged={(e) => setSelectedRoomId(e.detail.value)}
              className="w-full"
            />
          </>
        )}
      </div>
    </Dialog>
  );
};
