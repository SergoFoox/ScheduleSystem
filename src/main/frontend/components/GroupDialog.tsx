import React, { useEffect, useState } from 'react';
import { Dialog } from '@vaadin/react-components/Dialog.js';
import { TextField } from '@vaadin/react-components/TextField.js';
import { IntegerField } from '@vaadin/react-components/IntegerField.js';
import { Button } from '@vaadin/react-components/Button.js';
import { Icon } from '@vaadin/react-components/Icon.js';
import { FormLayout } from '@vaadin/react-components/FormLayout.js';
import { HorizontalLayout } from '@vaadin/react-components/HorizontalLayout.js';
import { Notification } from '@vaadin/react-components/Notification.js';
import { GroupEndpoint, TeacherEndpoint } from '../generated/endpoints';
import type GroupDTO from '../generated/com/sergofoox/domain/ui/dto/GroupDTO';
import type TeacherDTO from '../generated/com/sergofoox/domain/ui/dto/TeacherDTO';
import { Select } from '@vaadin/react-components/Select.js';

interface GroupDialogProps {
  opened: boolean;
  group?: GroupDTO;
  onClose: () => void;
  onSaved: () => void;
}

export const GroupDialog: React.FC<GroupDialogProps> = ({ opened, group, onClose, onSaved }) => {
  const [formData, setFormData] = useState<any>({
    id: undefined,
    name: '',
    size: 25,
    course: 1,
    department: '',
    curatorId: undefined
  });

  const [teachers, setTeachers] = useState<TeacherDTO[]>([]);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    TeacherEndpoint.getAllTeachers().then(data => setTeachers((data || []).filter(t => !!t) as TeacherDTO[]));
  }, []);

  useEffect(() => {
    if (group) {
      setFormData(group);
    } else {
      setFormData({
        id: undefined,
        name: '',
        size: 25,
        course: 1,
        department: '',
        curatorId: undefined
      });
    }
  }, [group, opened]);

  const handleSave = async () => {
    if (!formData.name || !formData.department) {
      Notification.show('Будь ласка, заповніть назву та кафедру', { theme: 'error' });
      return;
    }

    setSaving(true);
    try {
      await GroupEndpoint.saveGroup(formData as any);
      Notification.show(group ? 'Групу оновлено' : 'Групу створено', { theme: 'success' });
      onSaved();
      onClose();
    } catch (err) {
      console.error('Failed to save group:', err);
      Notification.show('Помилка при збереженні', { theme: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!group?.id) return;
    if (!window.confirm(`Ви впевнені, що хочете видалити групу "${group.name}"?`)) return;

    setSaving(true);
    try {
      await GroupEndpoint.deleteGroup(group.id as any);
      Notification.show('Групу видалено', { theme: 'success' });
      onSaved();
      onClose();
    } catch (err) {
      console.error('Failed to delete group:', err);
      Notification.show('Помилка при видаленні', { theme: 'error' });
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog
      headerTitle={group ? 'Редагування групи' : 'Додавання групи'}
      opened={opened}
      onOpenedChanged={(e) => !e.detail.value && onClose()}
      footerRenderer={() => (
        <HorizontalLayout className="w-full justify-between items-center px-4 py-2">
          <div>
            {group?.id && (
              <Button theme="error tertiary" onClick={handleDelete} disabled={saving}>
                <Icon icon="vaadin:trash" slot="prefix" />
                Видалити групу
              </Button>
            )}
          </div>
          <HorizontalLayout theme="spacing">
            <Button theme="primary" onClick={handleSave} disabled={saving}>
              {saving ? 'Збереження...' : 'Зберегти'}
            </Button>
            <Button onClick={onClose} disabled={saving}>Скасувати</Button>
          </HorizontalLayout>
        </HorizontalLayout>
      )}
    >
      <FormLayout style={{ width: '400px', maxWidth: '100%' }}>
        <TextField
          label="Назва групи"
          required
          value={formData.name || ''}
          onValueChanged={(e) => setFormData({ ...formData, name: e.detail.value })}
        />
        <IntegerField
          label="Курс"
          required
          value={formData.course?.toString()}
          onValueChanged={(e) => setFormData({ ...formData, course: e.detail.value ? parseInt(e.detail.value, 10) : 1 })}
          min={1}
          max={6}
          stepButtonsVisible
        />
        <IntegerField
          label="Кількість студентів"
          required
          value={formData.size?.toString()}
          onValueChanged={(e) => setFormData({ ...formData, size: e.detail.value ? parseInt(e.detail.value, 10) : 1 })}
          min={1}
          stepButtonsVisible
        />
        <TextField
          label="Кафедра"
          required
          value={formData.department || ''}
          onValueChanged={(e) => setFormData({ ...formData, department: e.detail.value })}
        />
        <Select
          label="Куратор групи"
          items={teachers.map(t => ({ label: t.fullName, value: t.id?.toString() }))}
          value={formData.curatorId?.toString()}
          onValueChanged={(e) => setFormData({ ...formData, curatorId: e.detail.value ? parseInt(e.detail.value, 10) : undefined })}
        />
      </FormLayout>
    </Dialog>
  );
};
