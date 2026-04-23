import React, { useState, useEffect } from 'react';
import { Dialog } from '@vaadin/react-components/Dialog.js';
import { Button } from '@vaadin/react-components/Button.js';
import { Select } from '@vaadin/react-components/Select.js';
import { Notification } from '@vaadin/react-components/Notification.js';
import { ScheduleEndpoint, CoursePlanEndpoint, RoomEndpoint, TeacherCompetenceMatrixEndpoint } from '../generated/endpoints';
import { refreshSchedule } from '../store/app-state';
import type CoursePlanDTO from '../generated/com/sergofoox/domain/ui/dto/CoursePlanDTO';
import type RoomDTO from '../generated/com/sergofoox/domain/ui/dto/RoomDTO';
import type TeacherCompetenceDTO from '../generated/com/sergofoox/domain/ui/dto/TeacherCompetenceDTO';

interface AssignLessonDialogProps {
  opened: boolean;
  day: string;
  lessonNum: number;
  groupId: number;
  timeslotId?: number;
  onClose: () => void;
}

export const AssignLessonDialog: React.FC<AssignLessonDialogProps> = ({ opened, day, lessonNum, groupId, timeslotId, onClose }) => {
  const [plans, setPlans] = useState<CoursePlanDTO[]>([]);
  const [rooms, setRooms] = useState<RoomDTO[]>([]);
  const [availableTeachers, setAvailableTeachers] = useState<any[]>([]);
  
  const [selectedSubjectId, setSelectedSubjectId] = useState<string | undefined>(undefined);
  const [selectedRoomId, setSelectedRoomId] = useState<string | undefined>(undefined);
  const [selectedTeacherId, setSelectedTeacherId] = useState<string | undefined>(undefined);
  
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (opened && groupId) {
      setLoading(true);
      Promise.all([
        CoursePlanEndpoint.getPlansByGroup(groupId as any),
        RoomEndpoint.getAllRooms()
      ]).then(([plansData, roomsData]) => {
        setPlans((plansData || []).filter(p => !!p) as CoursePlanDTO[]);
        setRooms((roomsData || []).filter(r => !!r) as RoomDTO[]);
      }).finally(() => setLoading(false));
    }
  }, [opened, groupId]);

  // Load teachers when subject changes
  useEffect(() => {
    if (selectedSubjectId) {
      // For now, let's fetch all teachers who have competence for this subject
      // In a real app, we'd have an endpoint for this. 
      // Let's reuse TeacherCompetenceMatrix logic
      // We'll just show a placeholder or fetch if we had the right endpoint.
      // Wait, we can fetch candidates via Replacement logic!
      ScheduleEndpoint.getScheduleGridData().then(data => {
        // Mocking teacher list from grid data if needed, or better:
        // Let's assume we need an endpoint. Since we don't have "getTeachersBySubject", 
        // we'll allow selecting any teacher for now to unblock the user.
        ScheduleEndpoint.getScheduleGridData().then(data => {
           setAvailableTeachers(data.teachers || []);
        });
      });
    }
  }, [selectedSubjectId]);

  const handleAssign = async () => {
    if (!selectedSubjectId || !timeslotId) return;
    setSaving(true);
    try {
      await ScheduleEndpoint.assignManualLesson(
        groupId as any, 
        parseInt(selectedSubjectId) as any, 
        timeslotId as any,
        selectedRoomId ? parseInt(selectedRoomId) as any : undefined,
        selectedTeacherId ? parseInt(selectedTeacherId) as any : undefined
      );
      Notification.show('Пару успішно призначено', { theme: 'success' });
      await refreshSchedule();
      onClose();
    } catch (err) {
      console.error(err);
      Notification.show('Помилка при призначенні пари', { theme: 'error' });
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog
      headerTitle={`Призначити пару (${day}, №${lessonNum})`}
      opened={opened}
      onOpenedChanged={(e) => !e.detail.value && onClose()}
      footerRenderer={() => (
        <div className="flex gap-2 p-2">
          <Button theme="primary" onClick={handleAssign} disabled={saving || !selectedSubjectId || loading}>
            Призначити
          </Button>
          <Button onClick={onClose}>Скасувати</Button>
        </div>
      )}
    >
      <div className="p-4 flex flex-col gap-4 min-w-[400px]">
        {loading ? (
          <p>Завантаження даних...</p>
        ) : plans.length === 0 ? (
          <p className="text-gray-500 italic">Для цієї групи не налаштовано навчальний план.</p>
        ) : (
          <>
            <Select
              label="1. Оберіть дисципліну"
              items={plans.map((p: any) => ({
                label: p.subjectName,
                value: p.subjectId.toString()
              }))}
              value={selectedSubjectId}
              onValueChanged={(e) => setSelectedSubjectId(e.detail.value)}
              className="w-full"
            />
            
            <Select
              label="2. Оберіть викладача"
              items={availableTeachers.map((t: any) => ({
                label: t.fullName,
                value: t.id.toString()
              }))}
              value={selectedTeacherId}
              onValueChanged={(e) => setSelectedTeacherId(e.detail.value)}
              className="w-full"
              disabled={!selectedSubjectId}
            />

            <Select
              label="3. Оберіть аудиторію"
              items={rooms.map((r: any) => ({
                label: `${r.name} (${r.type})`,
                value: r.id.toString()
              }))}
              value={selectedRoomId}
              onValueChanged={(e) => setSelectedRoomId(e.detail.value)}
              className="w-full"
              disabled={!selectedSubjectId}
            />
          </>
        )}
      </div>
    </Dialog>
  );
};
