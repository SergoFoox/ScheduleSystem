import React, { useEffect, useState } from 'react';
import { Dialog } from '@vaadin/react-components/Dialog.js';
import { Select } from '@vaadin/react-components/Select.js';
import { Button } from '@vaadin/react-components/Button.js';
import { Grid } from '@vaadin/react-components/Grid.js';
import { GridColumn } from '@vaadin/react-components/GridColumn.js';
import { Icon } from '@vaadin/react-components/Icon.js';
import { Notification } from '@vaadin/react-components/Notification.js';
import { TeacherCompetenceMatrixEndpoint, SubjectEndpoint } from '../generated/endpoints';
import type TeacherDTO from '../generated/com/sergofoox/domain/ui/dto/TeacherDTO';
import type TeacherCompetenceDTO from '../generated/com/sergofoox/domain/ui/dto/TeacherCompetenceDTO';
import Subject from '../generated/com/sergofoox/domain/subject/Subject';
import LessonType from '../generated/com/sergofoox/domain/subject/LessonType';
import Priority from '../generated/com/sergofoox/domain/competence/Priority';
import { HorizontalLayout, VerticalLayout } from '@vaadin/react-components';

interface CompetenceDialogProps {
  opened: boolean;
  teacher?: TeacherDTO;
  onClose: () => void;
}

export const CompetenceDialog: React.FC<CompetenceDialogProps> = ({ opened, teacher, onClose }) => {
  const [competences, setCompetences] = useState<TeacherCompetenceDTO[]>([]);
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [selectedSubjectId, setSelectedSubjectId] = useState<number | undefined>(undefined);
  const [selectedType, setSelectedType] = useState<LessonType>(LessonType.LECTURE);
  const [selectedPriority, setSelectedPriority] = useState<Priority>(Priority.SECONDARY);

  const refreshCompetences = async () => {
    if (teacher?.id) {
      const data = await TeacherCompetenceMatrixEndpoint.getCompetencesByTeacher(teacher.id);
      setCompetences((data || []).filter(item => !!item) as TeacherCompetenceDTO[]);
    }
  };

  useEffect(() => {
    if (opened && teacher?.id) {
      refreshCompetences();
      SubjectEndpoint.getAllSubjects().then(data => {
        setSubjects((data || []).filter(s => !!s) as Subject[]);
      });
    }
  }, [opened, teacher]);

  const handleAdd = async () => {
    if (!selectedSubjectId || !teacher?.id) return;
    try {
      await TeacherCompetenceMatrixEndpoint.saveCompetence({
        teacherId: teacher.id,
        subjectId: selectedSubjectId,
        lessonType: selectedType,
        priority: selectedPriority
      } as any);
      Notification.show('Компетенцію додано', { theme: 'success' });
      refreshCompetences();
    } catch (err) {
      console.error(err);
      Notification.show('Помилка додавання', { theme: 'error' });
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await TeacherCompetenceMatrixEndpoint.deleteCompetence(id);
      refreshCompetences();
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <Dialog
      headerTitle={`Компетенції: ${teacher?.fullName}`}
      opened={opened}
      onOpenedChanged={(e) => !e.detail.value && onClose()}
      className="w-[600px]"
    >
      <VerticalLayout className="gap-m p-m">
        <HorizontalLayout className="items-end gap-s w-full">
          <Select
            label="Предмет"
            items={subjects.map(s => ({ label: s.name, value: s.id?.toString() }))}
            onValueChanged={e => setSelectedSubjectId(e.detail.value ? parseInt(e.detail.value) : undefined)}
            className="flex-grow"
          />
          <Select
            label="Тип"
            items={[
              { label: 'Лекція', value: LessonType.LECTURE },
              { label: 'Практика', value: LessonType.PRACTICE },
              { label: 'Лаб.', value: LessonType.LABORATORY },
            ]}
            value={selectedType}
            onValueChanged={e => setSelectedType(e.detail.value as LessonType)}
            className="w-32"
          />
          <Select
            label="Пріоритет"
            items={[
              { label: 'Основний', value: Priority.PRIMARY },
              { label: 'Додатковий', value: Priority.SECONDARY },
              { label: 'Заміна', value: Priority.SUBSTITUTE },
            ]}
            value={selectedPriority}
            onValueChanged={e => setSelectedPriority(e.detail.value as Priority)}
            className="w-32"
          />
          <Button theme="primary" onClick={handleAdd}>
            <Icon icon="vaadin:plus" />
          </Button>
        </HorizontalLayout>

        <Grid items={competences} className="h-64 border rounded">
          <GridColumn path="subjectName" header="Дисципліна" autoWidth />
          <GridColumn path="lessonType" header="Тип" autoWidth />
          <GridColumn path="priority" header="Пріоритет" autoWidth />
          <GridColumn
            header="Дії"
            autoWidth
            renderer={({ item }) => (
              <Button theme="error icon" onClick={() => handleDelete(item.id!)}>
                <Icon icon="vaadin:trash" />
              </Button>
            )}
          />
        </Grid>
      </VerticalLayout>
    </Dialog>
  );
};
