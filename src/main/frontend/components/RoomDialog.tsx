import React, { useEffect, useState } from 'react';
import { Dialog } from '@vaadin/react-components/Dialog.js';
import { TextField } from '@vaadin/react-components/TextField.js';
import { IntegerField } from '@vaadin/react-components/IntegerField.js';
import { TextArea } from '@vaadin/react-components/TextArea.js';
import { Select } from '@vaadin/react-components/Select.js';
import { Button } from '@vaadin/react-components/Button.js';
import { FormLayout } from '@vaadin/react-components/FormLayout.js';
import { HorizontalLayout } from '@vaadin/react-components/HorizontalLayout.js';
import { Notification } from '@vaadin/react-components/Notification.js';
import { RoomEndpoint } from '../generated/endpoints';
import type RoomDTO from '../generated/com/sergofoox/domain/ui/dto/RoomDTO';
import RoomType from '../generated/com/sergofoox/domain/plan/RoomType';
import { getMutationErrorMessage } from '../store/app-state';
import { notifyDataChanged } from '../utils/cross-tab-sync';

interface RoomDialogProps {
  opened: boolean;
  room?: RoomDTO;
  onClose: () => void;
  onSaved: () => void;
}

export const RoomDialog: React.FC<RoomDialogProps> = ({ opened, room, onClose, onSaved }) => {
  const [formData, setFormData] = useState<RoomDTO>({
    id: undefined,
    name: '',
    capacity: 30,
    building: '',
    equipment: '',
    type: RoomType.GENERAL_CLASSROOM
  });

  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (room) {
      setFormData(room);
    } else {
      setFormData({
        id: undefined,
        name: '',
        capacity: 30,
        building: '',
        equipment: '',
        type: RoomType.GENERAL_CLASSROOM
      });
    }
  }, [room, opened]);

  const handleSave = async () => {
    if (!formData.name || !formData.building) {
      Notification.show('Будь ласка, заповніть обов\'язкові поля', { theme: 'error' });
      return;
    }

    setSaving(true);
    try {
      await RoomEndpoint.saveRoom(formData as any);
      Notification.show(room ? 'Аудиторію оновлено' : 'Аудиторію створено', { theme: 'success' });
      onSaved();
      notifyDataChanged('rooms');
      onClose();
    } catch (err) {
      console.error('Failed to save room:', err);
      Notification.show(getMutationErrorMessage(err, 'Помилка під час збереження'), { theme: 'error' });
    } finally {
      setSaving(false);
    }
  };

  const roomTypeOptions = [
    { label: 'Лекційна аудиторія', value: RoomType.LECTURE_HALL },
    { label: 'Лабораторія', value: RoomType.LABORATORY },
    { label: 'Комп\'ютерний клас', value: RoomType.COMPUTER_CLASS },
    { label: 'Загальна аудиторія', value: RoomType.GENERAL_CLASSROOM },
    { label: 'Спортивний зал', value: RoomType.SPORTS_HALL },
  ];

  return (
    <Dialog
      headerTitle={room ? 'Редагування аудиторії' : 'Додавання аудиторії'}
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
      <FormLayout style={{ width: '400px', maxWidth: '100%' }}>
        <TextField
          label="Назва аудиторії"
          required
          value={formData.name || ''}
          onValueChanged={(e) => setFormData({ ...formData, name: e.detail.value })}
        />
        <TextField
          label="Корпус"
          required
          value={formData.building || ''}
          onValueChanged={(e) => setFormData({ ...formData, building: e.detail.value })}
        />
        <IntegerField
          label="Місткість (місць)"
          value={formData.capacity?.toString()}
          onValueChanged={(e) => setFormData({ ...formData, capacity: e.detail.value ? parseInt(e.detail.value, 10) : undefined })}
          min={1}
          stepButtonsVisible
        />
        <Select
          label="Тип приміщення"
          items={roomTypeOptions}
          value={formData.type}
          onValueChanged={(e) => setFormData({ ...formData, type: e.detail.value as RoomType })}
        />
        <TextArea
          label="Обладнання"
          value={formData.equipment || ''}
          onValueChanged={(e) => setFormData({ ...formData, equipment: e.detail.value })}
          className="col-span-2"
        />
      </FormLayout>
    </Dialog>
  );
};
