import React, { useState, useEffect } from 'react';
import { Dialog } from '@vaadin/react-components/Dialog.js';
import { TextField } from '@vaadin/react-components/TextField.js';
import { Button } from '@vaadin/react-components/Button.js';
import { FormLayout } from '@vaadin/react-components/FormLayout.js';
import { HorizontalLayout } from '@vaadin/react-components/HorizontalLayout.js';
import { Notification } from '@vaadin/react-components/Notification.js';
import { SubjectEndpoint } from '../generated/endpoints';

import Subject from '../generated/com/sergofoox/domain/subject/Subject';

interface SubjectDialogProps {
  opened: boolean;
  subject?: Subject;
  onClose: () => void;
  onSaved: (newSubjectId: number) => void;
}

export const SubjectDialog: React.FC<SubjectDialogProps> = ({ opened, subject, onClose, onSaved }) => {
  const [name, setName] = useState('');
  const [abbreviation, setAbbreviation] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (subject) {
      setName(subject.name || '');
      setAbbreviation(subject.abbreviation || '');
    } else {
      setName('');
      setAbbreviation('');
    }
  }, [subject, opened]);

  const handleSave = async () => {
    if (!name || !abbreviation) {
      Notification.show('Заповніть назву та абревіатуру', { theme: 'error' });
      return;
    }
    setSaving(true);
    try {
      const subjectToSave = {
        ...subject,
        name,
        abbreviation
      };
      const savedSubject = await (SubjectEndpoint as any).saveSubject(subjectToSave);
      Notification.show(subject ? 'Дисципліну оновлено' : 'Дисципліну створено', { theme: 'success' });
      onSaved(savedSubject.id as any);
      onClose();
    } catch (err) {
      console.error(err);
      Notification.show('Помилка збереження дисципліни', { theme: 'error' });
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog
      headerTitle={subject ? "Редагування дисципліни" : "Нова дисципліна"}
      opened={opened}
      onOpenedChanged={(e) => !e.detail.value && onClose()}
      footerRenderer={() => (
        <HorizontalLayout theme="spacing" className="px-4 py-2">
          <Button theme="primary" onClick={handleSave} disabled={saving}>{subject ? 'Зберегти' : 'Створити'}</Button>
          <Button onClick={onClose}>Скасувати</Button>
        </HorizontalLayout>
      )}
    >
      <FormLayout>
        <TextField 
          label="Назва дисципліни" 
          value={name} 
          onValueChanged={e => setName(e.detail.value)} 
          required 
        />
        <TextField 
          label="Абревіатура (код)" 
          value={abbreviation} 
          onValueChanged={e => setAbbreviation(e.detail.value)} 
          placeholder="Напр. ПРОГ" 
          required 
        />
      </FormLayout>
    </Dialog>
  );
};
