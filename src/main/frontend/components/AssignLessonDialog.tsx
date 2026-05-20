import React, { useState, useEffect } from 'react';
import { Dialog } from '@vaadin/react-components/Dialog.js';
import { Button } from '@vaadin/react-components/Button.js';
import { Select } from '@vaadin/react-components/Select.js';
import { Notification } from '@vaadin/react-components/Notification.js';
import { Icon } from '@vaadin/react-components/Icon.js';
import { ScheduleEndpoint, CoursePlanEndpoint, RoomEndpoint } from '../generated/endpoints';
import { getMutationErrorMessage, refreshSchedule } from '../store/app-state';
import type CoursePlanDTO from '../generated/com/sergofoox/domain/ui/dto/CoursePlanDTO';
import type RoomDTO from '../generated/com/sergofoox/domain/ui/dto/RoomDTO';
import { notifyDataChanged } from '../utils/cross-tab-sync';

interface AssignLessonDialogProps {
  opened: boolean;
  day: string;
  lessonNum: number;
  groupId: number;
  timeslotId?: number;
  onClose: () => void;
}

interface AssignmentRow {
  teacherId: string | undefined;
  roomId: string | undefined;
  subgroup: string;
  periodicity: string;
}

const AssignLessonDialog: React.FC<AssignLessonDialogProps> = ({ opened, day, lessonNum, groupId, timeslotId, onClose }) => {
  const [plans, setPlans] = useState<CoursePlanDTO[]>([]);
  const [rooms, setRooms] = useState<RoomDTO[]>([]);
  const [availableTeachers, setAvailableTeachers] = useState<any[]>([]);
  
  const [selectedSubjectId, setSelectedSubjectId] = useState<string | undefined>(undefined);
  const [assignments, setAssignments] = useState<AssignmentRow[]>([
    { teacherId: undefined, roomId: undefined, subgroup: '0', periodicity: 'WEEKLY' }
  ]);
  
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (opened && groupId) {
      setLoading(true);
      Promise.all([
        CoursePlanEndpoint.getPlansByGroup(groupId as any),
        RoomEndpoint.getAllRooms(),
        ScheduleEndpoint.getScheduleGridData()
      ]).then(([plansData, roomsData, gridData]) => {
        setPlans((plansData || []).filter(p => !!p) as CoursePlanDTO[]);
        setRooms((roomsData || []).filter(r => !!r) as RoomDTO[]);
        setAvailableTeachers(gridData.teachers || []);
      }).finally(() => setLoading(false));
    }
  }, [opened, groupId]);

  // Load teachers when subject changes
  useEffect(() => {
    if (selectedSubjectId) {
      ScheduleEndpoint.getScheduleGridData().then(data => {
        setAvailableTeachers(data.teachers || []);
      });
    }
  }, [selectedSubjectId]);

  const addAssignmentRow = () => {
    setAssignments(prev => {
      if (prev.length === 1 && prev[0].subgroup === '0') {
        return [
          { ...prev[0], subgroup: '1' },
          { teacherId: undefined, roomId: undefined, subgroup: '2', periodicity: prev[0].periodicity }
        ];
      }
      return [...prev, { teacherId: undefined, roomId: undefined, subgroup: (prev.length + 1).toString(), periodicity: prev[0].periodicity }];
    });
  };

  const removeAssignmentRow = (index: number) => {
    if (assignments.length > 1) {
      const newRows = assignments.filter((_, i) => i !== index);
      if (newRows.length === 1) newRows[0].subgroup = '0';
      setAssignments(newRows);
    }
  };

  const updateRow = (index: number, field: keyof AssignmentRow, value: string | undefined) => {
    const newRows = [...assignments];
    newRows[index] = { ...newRows[index], [field]: value } as AssignmentRow;
    setAssignments(newRows);
  };

  const handleAssign = async () => {
    if (!selectedSubjectId || !timeslotId) return;
    
    const isValid = assignments.every(row => row.teacherId);
    if (!isValid) {
      Notification.show('Оберіть викладача для кожної підгрупи', { theme: 'error' });
      return;
    }

    setSaving(true);
    try {
      for (const row of assignments) {
        await (ScheduleEndpoint as any).assignManualLesson(
          groupId as any, 
          parseInt(selectedSubjectId) as any, 
          timeslotId as any,
          row.roomId ? parseInt(row.roomId) as any : undefined,
          row.teacherId ? parseInt(row.teacherId) as any : undefined,
          parseInt(row.subgroup),
          row.periodicity
        );
      }
      
      Notification.show('Заняття успішно призначено', { theme: 'success' });
      await refreshSchedule();
      notifyDataChanged('schedule');
      onClose();
    } catch (err) {
      console.error(err);
      Notification.show(getMutationErrorMessage(err, 'Помилка під час призначення заняття'), { theme: 'error' });
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog
      headerTitle={`Призначити заняття (${day}, №${lessonNum})`}
      opened={opened}
      onOpenedChanged={(e) => !e.detail.value && onClose()}
      footerRenderer={() => (
        <div className="flex gap-2 p-2 w-full justify-between items-center">
           <Button theme="tertiary" onClick={addAssignmentRow} disabled={loading || !selectedSubjectId}>
             <Icon icon="vaadin:plus" slot="prefix" />
             Додати підгрупу / паралельне заняття
          </Button>
          <div className="flex gap-2">
            <Button theme="primary" onClick={handleAssign} disabled={saving || !selectedSubjectId || loading}>
              Призначити всі
            </Button>
            <Button onClick={onClose}>Скасувати</Button>
          </div>
        </div>
      )}
    >
      <div className="p-4 flex flex-col gap-6 min-w-[700px]">
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
            
            <div className="flex flex-col gap-4 bg-gray-50 p-4 rounded-xl border">
              <h3 className="text-sm font-bold text-gray-700 uppercase tracking-wider">Призначення викладачів та аудиторій:</h3>
              
              {assignments.map((row, index) => (
                <div key={index} className="flex flex-col gap-2 bg-white p-3 rounded-lg border shadow-sm">
                  <div className="flex items-end gap-s w-full">
                    <Select
                      label="Склад"
                      items={[
                        { label: 'Уся група', value: '0' },
                        { label: '1 підгр.', value: '1' },
                        { label: '2 підгр.', value: '2' },
                        { label: '3 підгр.', value: '3' },
                      ]}
                      value={row.subgroup}
                      onValueChanged={e => updateRow(index, 'subgroup', e.detail.value)}
                      className="w-32"
                    />

                    <Select
                      label="Цикл (Ч/З)"
                      items={[
                        { label: 'Щотижня', value: 'WEEKLY' },
                        { label: 'Чисельник', value: 'ODD_WEEKS' },
                        { label: 'Знаменник', value: 'EVEN_WEEKS' },
                      ]}
                      value={row.periodicity}
                      onValueChanged={e => updateRow(index, 'periodicity', e.detail.value)}
                      className="w-40"
                    />

                    <div className="flex-grow" />

                    {assignments.length > 1 && (
                      <Button theme="error icon tertiary" onClick={() => removeAssignmentRow(index)}>
                        <Icon icon="vaadin:trash" />
                      </Button>
                    )}
                  </div>

                  <div className="flex items-end gap-s w-full">
                    <Select
                      label="Викладач"
                      items={availableTeachers.map((t: any) => ({
                        label: t.fullName,
                        value: t.id.toString()
                      }))}
                      value={row.teacherId}
                      onValueChanged={e => updateRow(index, 'teacherId', e.detail.value)}
                      className="flex-grow"
                      disabled={!selectedSubjectId}
                    />

                    <Select
                      label="Аудиторія"
                      items={rooms.map((r: any) => ({
                        label: `${r.name}`,
                        value: r.id.toString()
                      }))}
                      value={row.roomId}
                      onValueChanged={e => updateRow(index, 'roomId', e.detail.value)}
                      className="w-24"
                      disabled={!selectedSubjectId}
                    />
                  </div>
                </div>
              ))}
            </div>
          </>
        )}
      </div>
    </Dialog>
  );
};

export default AssignLessonDialog;
