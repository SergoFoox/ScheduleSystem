import { useEffect, useState } from 'react';
import { Button } from '@vaadin/react-components/Button.js';
import { Grid } from '@vaadin/react-components/Grid.js';
import { GridColumn } from '@vaadin/react-components/GridColumn.js';
import { Icon } from '@vaadin/react-components/Icon.js';
import { Notification } from '@vaadin/react-components/Notification.js';
import { SubjectEndpoint } from '../generated/endpoints';
import Subject from '../generated/com/sergofoox/domain/subject/Subject';
import { SubjectDialog } from '../components/SubjectDialog';

export default function SubjectsView() {
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [selectedSubject, setSelectedSubject] = useState<Subject | undefined>(undefined);
  const [dialogOpened, setDialogOpened] = useState(false);

  const refreshSubjects = async () => {
    try {
      const data = await SubjectEndpoint.getAllSubjects();
      setSubjects((data || []).filter(s => !!s) as Subject[]);
    } catch (err) {
      console.error(err);
      Notification.show('Помилка завантаження предметів', { theme: 'error' });
    }
  };

  useEffect(() => {
    refreshSubjects();
  }, []);

  const handleAdd = () => {
    setSelectedSubject(undefined);
    setDialogOpened(true);
  };

  const handleEdit = (subject: Subject) => {
    setSelectedSubject(subject);
    setDialogOpened(true);
  };

  const handleDelete = async (id: number) => {
    if (confirm('Ви впевнені, що хочете видалити цей предмет? Увага: всі пов’язані плани навантаження та заняття також будуть видалені!')) {
      try {
        await SubjectEndpoint.deleteSubject(id as any);
        Notification.show('Предмет та всі пов’язані дані видалено', { theme: 'success' });
        refreshSubjects();
      } catch (err) {
        console.error(err);
        Notification.show('Помилка видалення предмета', { theme: 'error' });
      }
    }
  };

  return (
    <div className="p-m flex flex-col gap-m h-full">
      <div className="flex justify-between items-center px-4 pt-4">
        <h2 className="text-xl font-bold">Дисципліни (Предмети)</h2>
        <Button theme="primary" onClick={handleAdd}>
          <Icon icon="vaadin:plus" slot="prefix" />
          Додати предмет
        </Button>
      </div>

      <div className="flex-grow overflow-auto px-4">
        <Grid 
          items={subjects} 
          className="h-full border rounded-lg shadow-sm"
        >
          <GridColumn path="name" header="Назва дисципліни" resizable autoWidth />
          <GridColumn path="abbreviation" header="Абревіатура" autoWidth />
          <GridColumn
            header="Дії"
            autoWidth
            frozenToEnd
            renderer={({ item }) => (
              <div className="flex gap-2">
                <Button
                  theme="tertiary icon"
                  aria-label="Edit"
                  onClick={() => handleEdit(item as Subject)}
                  title="Редагувати"
                >
                  <Icon icon="vaadin:edit" />
                </Button>
                <Button
                  theme="error tertiary icon"
                  aria-label="Delete"
                  onClick={() => handleDelete(item.id!)}
                  title="Видалити"
                >
                  <Icon icon="vaadin:trash" />
                </Button>
              </div>
            )}
          />
        </Grid>
      </div>

      <SubjectDialog
        opened={dialogOpened}
        subject={selectedSubject}
        onClose={() => {
          setDialogOpened(false);
          setSelectedSubject(undefined);
        }}
        onSaved={() => refreshSubjects()}
      />
    </div>
  );
}
