import React, { useEffect, useState } from 'react';
import { Dialog } from '@vaadin/react-components/Dialog.js';
import { TextField } from '@vaadin/react-components/TextField.js';
import { IntegerField } from '@vaadin/react-components/IntegerField.js';
import { Select } from '@vaadin/react-components/Select.js';
import { Button } from '@vaadin/react-components/Button.js';
import { FormLayout } from '@vaadin/react-components/FormLayout.js';
import { HorizontalLayout } from '@vaadin/react-components/HorizontalLayout.js';
import { Notification } from '@vaadin/react-components/Notification.js';
import { RoomEndpoint, TeacherEndpoint } from '../generated/endpoints';
import type TeacherDTO from '../generated/com/sergofoox/domain/ui/dto/TeacherDTO';
import type RoomDTO from '../generated/com/sergofoox/domain/ui/dto/RoomDTO';
import PositionType from '../generated/com/sergofoox/domain/teacher/PositionType';

interface TeacherDialogProps {
  opened: boolean;
  teacher?: TeacherDTO;
  onClose: () => void;
  onSaved: () => void;
}

export const TeacherDialog: React.FC<TeacherDialogProps> = ({ opened, teacher, onClose, onSaved }) => {
  const [formData, setFormData] = useState<TeacherDTO>({
    id: undefined,
    fullName: '',
    department: '',
    positionType: PositionType.FULL_TIME,
    weeklyHourLimit: 40,
    maxWorkingDaysPerWeek: 5,
    assignedRoomId: undefined,
    assignedRoomName: undefined
  });

  const [saving, setSaving] = useState(false);
  const [rooms, setRooms] = useState<RoomDTO[]>([]);

  useEffect(() => {
    RoomEndpoint.getAllRooms()
      .then(data => setRooms((data || []).filter(room => !!room) as RoomDTO[]))
      .catch(err => console.error('Failed to load rooms:', err));
  }, [opened]);

  useEffect(() => {
    if (teacher) {
      setFormData({
        ...teacher,
        maxWorkingDaysPerWeek: teacher.maxWorkingDaysPerWeek ?? 5
      });
    } else {
      setFormData({
        id: undefined,
        fullName: '',
        department: '',
        positionType: PositionType.FULL_TIME,
        weeklyHourLimit: 40,
        maxWorkingDaysPerWeek: 5,
        assignedRoomId: undefined,
        assignedRoomName: undefined
      });
    }
  }, [teacher, opened]);

  const handleSave = async () => {
    if (!formData.fullName || !formData.department) {
      Notification.show('Будь ласка, заповніть ПІБ та кафедру', { theme: 'error' });
      return;
    }

    setSaving(true);
    try {
      await TeacherEndpoint.saveTeacher(formData as any);
      Notification.show(teacher ? 'Дані викладача оновлено' : 'Викладача додано', { theme: 'success' });
      onSaved();
      onClose();
    } catch (err) {
      console.error('Failed to save teacher:', err);
      Notification.show('Помилка при збереженні', { theme: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const positionOptions = [
    { label: 'Штатний викладач', value: PositionType.FULL_TIME },
    { label: 'Сумісник', value: PositionType.PART_TIME },
    { label: 'За договором', value: PositionType.CONTRACT },
  ];

  return (
    <Dialog
      headerTitle={teacher ? 'Редагування даних викладача' : 'Додавання викладача'}
      opened={opened}
      onOpenedChanged={(e) => !e.detail.value && onClose()}
      footerRenderer={() => (
        <HorizontalLayout theme="spacing">
          <Button theme="primary" onClick={handleSave} disabled={saving}>
            {saving ? 'Збереження...' : 'Зберегти'}
          </Button>
          <Button onClick={onClose}>Скасувати</Button>
        </HorizontalLayout>
      )}
    >
      <FormLayout style={{ width: '450px', maxWidth: '100%' }}>
        <TextField
          label="ПІБ викладача"
          required
          value={formData.fullName || ''}
          onValueChanged={(e) => setFormData({ ...formData, fullName: e.detail.value })}
          className="col-span-2"
        />
        <TextField
          label="Кафедра"
          required
          value={formData.department || ''}
          onValueChanged={(e) => setFormData({ ...formData, department: e.detail.value })}
        />
        <TextField
          label="Спеціалізація (основна дисципліна)"
          placeholder="Напр. Вища математика"
          value={formData.specialization || ''}
          onValueChanged={(e) => setFormData({ ...formData, specialization: e.detail.value })}
        />
        <Select
          label="Посада"
          items={positionOptions}
          value={formData.positionType}
          onValueChanged={(e) => setFormData({ ...formData, positionType: e.detail.value as PositionType })}
        />
        <Select
          label="Закріплена аудиторія"
          items={[
            { label: 'Без привʼязки', value: '' },
            ...rooms.map(room => ({ label: `${room.name}${room.building ? ` (${room.building})` : ''}`, value: room.id?.toString() || '' }))
          ]}
          value={formData.assignedRoomId?.toString() || ''}
          onValueChanged={(e) => setFormData({ ...formData, assignedRoomId: e.detail.value ? parseInt(e.detail.value, 10) : undefined })}
          className="col-span-2"
        />
        <IntegerField
          label="Ліміт годин на тиждень"
          value={formData.weeklyHourLimit?.toString()}
          onValueChanged={(e) => setFormData({ ...formData, weeklyHourLimit: e.detail.value ? parseInt(e.detail.value, 10) : 40 })}
          min={0}
          max={100}
          stepButtonsVisible
          className="col-span-2"
        />
        <IntegerField
          label="Макс. робочих днів на тиждень"
          value={formData.maxWorkingDaysPerWeek?.toString()}
          onValueChanged={(e) => setFormData({ ...formData, maxWorkingDaysPerWeek: e.detail.value ? parseInt(e.detail.value, 10) : 5 })}
          min={1}
          max={6}
          stepButtonsVisible
          className="col-span-2"
        />
      </FormLayout>
    </Dialog>
  );
};
