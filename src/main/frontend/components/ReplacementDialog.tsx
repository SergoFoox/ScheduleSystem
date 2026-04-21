import React, { useEffect } from 'react';
import { Dialog } from '@vaadin/react-components/Dialog.js';
import { Grid } from '@vaadin/react-components/Grid.js';
import { GridColumn } from '@vaadin/react-components/GridColumn.js';
import { Button } from '@vaadin/react-components/Button.js';
import { VerticalLayout } from '@vaadin/react-components/VerticalLayout.js';
import { ScheduleEndpoint } from '../generated/endpoints';
import type ReplacementCandidateDTO from '../generated/com/sergofoox/domain/ui/dto/ReplacementCandidateDTO';
import type LessonDTO from '../generated/com/sergofoox/domain/ui/dto/LessonDTO';
import { useSignal } from '@vaadin/hilla-react-signals';
import { refreshSchedule } from '../store/app-state';
import Priority from '../generated/com/sergofoox/domain/competence/Priority';

interface ReplacementDialogProps {
  lesson: LessonDTO;
  opened: boolean;
  onClose: () => void;
}

const translatePriority = (priority: Priority | undefined) => {
  switch (priority) {
    case Priority.PRIMARY: return 'Основний';
    case Priority.SECONDARY: return 'Другорядний';
    case Priority.SUBSTITUTE: return 'Заміна';
    default: return 'Невідомо';
  }
};

export const ReplacementDialog: React.FC<ReplacementDialogProps> = ({ lesson, opened, onClose }) => {
  const candidates = useSignal<ReplacementCandidateDTO[]>([]);
  const loading = useSignal(true);

  useEffect(() => {
    if (opened && lesson.id) {
      loading.value = true;
      ScheduleEndpoint.getReplacementCandidates(lesson.id as any)
        .then(result => {
          candidates.value = result;
        })
        .catch(err => {
          console.error('Failed to load candidates:', err);
        })
        .finally(() => {
          loading.value = false;
        });
    }
  }, [opened, lesson.id]);

  const handleSelect = async (teacherId: number) => {
    if (!lesson.id) return;
    try {
      await ScheduleEndpoint.assignReplacement(lesson.id as any, teacherId as any);
      await refreshSchedule();
      onClose();
    } catch (err) {
      console.error('Failed to assign replacement:', err);
      alert('Помилка при призначенні заміни');
    }
  };

  return (
    <Dialog
      headerTitle="Підбір заміни"
      opened={opened}
      onOpenedChanged={(e) => !e.detail.value && onClose()}
      footerRenderer={() => (
        <Button onClick={onClose}>Скасувати</Button>
      )}
    >
      <VerticalLayout style={{ alignItems: 'stretch', width: '700px', maxWidth: '100%' }}>
        <div className="mb-4 p-2 rounded-md bg-gray-50 border">
          <div className="font-bold text-m">{lesson.subjectName}</div>
          <div className="text-gray-500 text-s">
            Група: {lesson.groupName} • Аудиторія: {lesson.roomName}
          </div>
          <div className="text-gray-500 text-s mt-1 italic">
            Поточний викладач: {lesson.teacherName}
          </div>
        </div>

        {loading.value ? (
          <div className="p-4 text-center">Завантаження...</div>
        ) : candidates.value.length === 0 ? (
          <div className="p-4 text-center text-gray-500 border rounded-md dashed">
            Немає доступних кандидатів
          </div>
        ) : (
          <Grid items={candidates.value} allRowsVisible>
            <GridColumn header="Викладач" path="fullName" />
            <GridColumn 
              header="Пріоритет" 
              renderer={({ item }) => (
                <span className={`px-2 py-1 rounded text-xs font-bold ${
                  item.priority === Priority.PRIMARY ? 'bg-green-50 text-green-500' :
                  item.priority === Priority.SECONDARY ? 'bg-blue-50 text-blue-600' : 'bg-gray-100'
                }`}>
                  {translatePriority(item.priority)}
                </span>
              )}
            />
            <GridColumn 
              header="Навантаження (день)" 
              renderer={({ item }) => (
                <span className="text-s">
                  {item.currentWorkload} занять
                </span>
              )}
            />
            <GridColumn
              header="Дія"
              renderer={({ item }) => (
                <Button 
                  theme="primary small" 
                  onClick={() => item.id && handleSelect(item.id as any)}
                >
                  Призначити заміну
                </Button>
              )}
            />
          </Grid>
        )}
      </VerticalLayout>
    </Dialog>
  );
};
