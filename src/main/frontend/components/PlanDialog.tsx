import React, { useEffect, useState } from 'react';
import { Dialog } from '@vaadin/react-components/Dialog.js';
import { Select } from '@vaadin/react-components/Select.js';
import { IntegerField } from '@vaadin/react-components/IntegerField.js';
import { Button } from '@vaadin/react-components/Button.js';
import { FormLayout } from '@vaadin/react-components/FormLayout.js';
import { HorizontalLayout } from '@vaadin/react-components/HorizontalLayout.js';
import { Notification } from '@vaadin/react-components/Notification.js';
import { CoursePlanEndpoint, GroupEndpoint, SubjectEndpoint } from '../generated/endpoints';
import type CoursePlanDTO from '../generated/com/sergofoox/domain/ui/dto/CoursePlanDTO';
import type GroupDTO from '../generated/com/sergofoox/domain/ui/dto/GroupDTO';
import Subject from '../generated/com/sergofoox/domain/subject/Subject';
import RoomType from '../generated/com/sergofoox/domain/plan/RoomType';

interface PlanDialogProps {
  opened: boolean;
  plan?: CoursePlanDTO;
  onClose: () => void;
  onSaved: () => void;
}

export const PlanDialog: React.FC<PlanDialogProps> = ({ opened, plan, onClose, onSaved }) => {
  const [groups, setGroups] = useState<GroupDTO[]>([]);
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [saving, setSaving] = useState(false);

  const [formData, setFormData] = useState<Partial<CoursePlanDTO>>({
    groupId: undefined,
    subjectId: undefined,
    lectureHours: 16,
    practiceHours: 16,
    labHours: 0,
    totalHours: 32,
    requiredRoomType: RoomType.GENERAL_CLASSROOM
  });

  useEffect(() => {
    if (plan) {
      setFormData(plan);
    }
  }, [plan]);

  useEffect(() => {
    Promise.all([
      GroupEndpoint.getAllGroups(),
      SubjectEndpoint.getAllSubjects()
    ]).then(([gData, sData]) => {
      setGroups((gData || []).filter(g => !!g) as GroupDTO[]);
      setSubjects((sData || []).filter(s => !!s) as Subject[]);
    });
  }, []);

  const handleSave = async () => {
    if (!formData.groupId || !formData.subjectId) {
      Notification.show('Оберіть групу та предмет', { theme: 'error' });
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
          disabled={!!plan}
        />
        <Select
          label="Дисципліна"
          items={subjects.map(s => ({ label: s.name, value: s.id?.toString() }))}
          value={formData.subjectId?.toString()}
          onValueChanged={(e) => setFormData({...formData, subjectId: e.detail.value ? parseInt(e.detail.value) : undefined})}
          disabled={!!plan}
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
