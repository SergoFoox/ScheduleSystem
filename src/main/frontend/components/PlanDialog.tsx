import React, { useEffect, useState } from 'react';
import { Dialog } from '@vaadin/react-components/Dialog.js';
import { Select } from '@vaadin/react-components/Select.js';
import { IntegerField } from '@vaadin/react-components/IntegerField.js';
import { Button } from '@vaadin/react-components/Button.js';
import { FormLayout } from '@vaadin/react-components/FormLayout.js';
import { HorizontalLayout } from '@vaadin/react-components/HorizontalLayout.js';
import { Notification } from '@vaadin/react-components/Notification.js';
import { CoursePlanEndpoint, GroupEndpoint, SubjectEndpoint, TeacherEndpoint } from '../generated/endpoints';
import type CoursePlanDTO from '../generated/com/sergofoox/domain/ui/dto/CoursePlanDTO';
import type GroupDTO from '../generated/com/sergofoox/domain/ui/dto/GroupDTO';
import type TeacherDTO from '../generated/com/sergofoox/domain/ui/dto/TeacherDTO';
import Subject from '../generated/com/sergofoox/domain/subject/Subject';
import RoomType from '../generated/com/sergofoox/domain/plan/RoomType';

interface PlanDialogProps {
  opened: boolean;
  plan?: CoursePlanDTO;
  defaultGroupId?: number;
  onClose: () => void;
  onSaved: () => void;
}

const createDefaultFormData = (groupId?: number): Partial<CoursePlanDTO> => ({
  groupId,
  subjectId: undefined,
  teacherId: undefined,
  secondTeacherId: undefined,
  lectureHours: 16,
  practiceHours: 16,
  labHours: 0,
  totalHours: 32,
  requiredRoomType: RoomType.GENERAL_CLASSROOM
});

export const PlanDialog: React.FC<PlanDialogProps> = ({ opened, plan, defaultGroupId, onClose, onSaved }) => {
  const [groups, setGroups] = useState<GroupDTO[]>([]);
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [teachers, setTeachers] = useState<TeacherDTO[]>([]);
  const [saving, setSaving] = useState(false);

  const [formData, setFormData] = useState<Partial<CoursePlanDTO>>(createDefaultFormData(defaultGroupId));

  useEffect(() => {
    if (!opened) return;
    if (plan) {
      setFormData(plan);
    } else {
      setFormData(createDefaultFormData(defaultGroupId));
    }
  }, [opened, plan, defaultGroupId]);

  useEffect(() => {
    Promise.all([
      GroupEndpoint.getAllGroups(),
      SubjectEndpoint.getAllSubjects(),
      TeacherEndpoint.getAllTeachers()
    ]).then(([gData, sData, tData]) => {
      setGroups((gData || []).filter(g => !!g) as GroupDTO[]);
      setSubjects((sData || []).filter(s => !!s) as Subject[]);
      setTeachers((tData || []).filter(t => !!t) as TeacherDTO[]);
    });
  }, []);

  const handleSave = async () => {
    if (!formData.groupId || !formData.subjectId) {
      Notification.show('Оберіть групу та дисципліну', { theme: 'error' });
      return;
    }
    if (!formData.teacherId) {
      Notification.show('Оберіть викладача', { theme: 'error' });
      return;
    }
    setSaving(true);
    try {
      const dataToSave = {
        ...formData,
        totalHours: (formData.lectureHours || 0) + (formData.practiceHours || 0) + (formData.labHours || 0)
      };
      await CoursePlanEndpoint.savePlan(dataToSave as CoursePlanDTO);
      Notification.show('План збережено', { theme: 'success' });
      onSaved();
      onClose();
    } catch (err) {
      console.error(err);
      Notification.show('Помилка збереження', { theme: 'error' });
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog
      headerTitle={plan ? "Редагування навантаження" : "Додавання дисципліни до плану"}
      opened={opened}
      onOpenedChanged={(e) => !e.detail.value && onClose()}
      footerRenderer={() => (
        <HorizontalLayout theme="spacing" className="px-4 py-2">
          <Button theme="primary" onClick={handleSave} disabled={saving}>Зберегти</Button>
          <Button onClick={onClose}>Скасувати</Button>
        </HorizontalLayout>
      )}
    >
      <FormLayout style={{ width: '450px' }}>
        <Select
          label="Група"
          items={groups.map(g => ({ label: g.name, value: g.id?.toString() }))}
          value={formData.groupId?.toString()}
          onValueChanged={(e) => setFormData({...formData, groupId: e.detail.value ? parseInt(e.detail.value) : undefined})}
          disabled={!!plan || !!defaultGroupId}
        />
        <Select
          label="Дисципліна"
          items={subjects.map(s => ({ label: s.name, value: s.id?.toString() }))}
          value={formData.subjectId?.toString()}
          onValueChanged={(e) => setFormData({...formData, subjectId: e.detail.value ? parseInt(e.detail.value) : undefined})}
          disabled={!!plan}
        />
        <Select
          label="Викладач"
          items={teachers.map(t => ({ label: t.fullName, value: t.id?.toString() }))}
          value={formData.teacherId?.toString()}
          onValueChanged={(e) => {
            const teacherId = e.detail.value ? parseInt(e.detail.value, 10) : undefined;
            setFormData({
              ...formData,
              teacherId,
              secondTeacherId: formData.secondTeacherId === teacherId ? undefined : formData.secondTeacherId
            });
          }}
        />
        <Select
          label="Другий викладач"
          items={[
            { label: 'Без другого викладача', value: '' },
            ...teachers
              .filter(t => t.id !== formData.teacherId)
              .map(t => ({ label: t.fullName, value: t.id?.toString() }))
          ]}
          value={formData.secondTeacherId?.toString() || ''}
          onValueChanged={(e) => setFormData({...formData, secondTeacherId: e.detail.value ? parseInt(e.detail.value, 10) : undefined})}
        />
        <Select
          label="Тип аудиторії"
          items={[
            { label: 'Загальна', value: RoomType.GENERAL_CLASSROOM },
            { label: 'Лекційна', value: RoomType.LECTURE_HALL },
            { label: 'Лабораторія', value: RoomType.LABORATORY },
            { label: 'Комп. клас', value: RoomType.COMPUTER_CLASS },
          ]}
          value={formData.requiredRoomType}
          onValueChanged={(e) => setFormData({...formData, requiredRoomType: e.detail.value as RoomType})}
        />
        <IntegerField
          label="Лекції (год)"
          value={formData.lectureHours?.toString()}
          onValueChanged={(e) => setFormData({...formData, lectureHours: parseInt(e.detail.value || '0')})}
        />
        <IntegerField
          label="Практики (год)"
          value={formData.practiceHours?.toString()}
          onValueChanged={(e) => setFormData({...formData, practiceHours: parseInt(e.detail.value || '0')})}
        />
        <IntegerField
          label="Лаб. (год)"
          value={formData.labHours?.toString()}
          onValueChanged={(e) => setFormData({...formData, labHours: parseInt(e.detail.value || '0')})}
        />
      </FormLayout>
    </Dialog>
  );
};
