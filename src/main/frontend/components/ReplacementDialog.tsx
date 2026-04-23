import React, { useEffect, useState } from 'react';
import { Dialog } from '@vaadin/react-components/Dialog.js';
import { Grid } from '@vaadin/react-components/Grid.js';
import { GridColumn } from '@vaadin/react-components/GridColumn.js';
import { Button } from '@vaadin/react-components/Button.js';
import { Select } from '@vaadin/react-components/Select.js';
import { VerticalLayout, HorizontalLayout } from '@vaadin/react-components';
import { ScheduleEndpoint, RoomEndpoint } from '../generated/endpoints';
import type ReplacementCandidateDTO from '../generated/com/sergofoox/domain/ui/dto/ReplacementCandidateDTO';
import type LessonDTO from '../generated/com/sergofoox/domain/ui/dto/LessonDTO';
import type RoomDTO from '../generated/com/sergofoox/domain/ui/dto/RoomDTO';
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
  const rooms = useSignal<RoomDTO[]>([]);
  const selectedRoomId = useSignal<string | undefined>(undefined);
  const loading = useSignal(true);

  useEffect(() => {
    if (opened && lesson.id) {
      loading.value = true;
      selectedRoomId.value = lesson.roomId?.toString();
      
      Promise.all([
        ScheduleEndpoint.getReplacementCandidates(lesson.id as any),
        RoomEndpoint.getAllRooms()
      ]).then(([candidateData, roomData]) => {
        candidates.value = candidateData;
        rooms.value = (roomData || []).filter(r => !!r) as RoomDTO[];
      }).catch(err => {
        console.error('Failed to load data:', err);
      }).finally(() => {
        loading.value = false;
      });
    }
  }, [opened, lesson.id]);

  const handleSelect = async (teacherId: number) => {
    if (!lesson.id) return;
    try {
      await ScheduleEndpoint.assignReplacement(
        lesson.id as any, 
        teacherId as any, 
        selectedRoomId.value ? parseInt(selectedRoomId.value) as any : undefined
      );
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
      <VerticalLayout style={{ alignItems: 'stretch', width: '750px', maxWidth: '100%' }}>
        <HorizontalLayout className="gap-m mb-4 p-4 rounded-xl bg-blue-50 border border-blue-100 shadow-sm items-center justify-between">
          <VerticalLayout className="gap-1 p-0">
            <div className="font-extrabold text-lg text-blue-900 uppercase tracking-tight">{lesson.subjectName}</div>
            <div className="text-blue-700 text-sm font-medium">
               {lesson.groupName} • {lesson.teacherName}
            </div>
          </VerticalLayout>
          
          <div className="w-64">
            <Select
              label="Змінити аудиторію"
              items={rooms.value.map(r => ({ label: `${r.name} (${r.type})`, value: r.id?.toString() }))}
              value={selectedRoomId.value}
              onValueChanged={e => selectedRoomId.value = e.detail.value}
              className="w-full"
            />
          </div>
        </HorizontalLayout>

        {loading.value ? (
          <div className="p-8 text-center text-gray-500 animate-pulse">Пошук вільних викладачів...</div>
        ) : candidates.value.length === 0 ? (
          <div className="p-12 text-center text-gray-400 border-2 border-dashed rounded-xl">
            Немає доступних викладачів для заміни
          </div>
        ) : (
          <Grid items={candidates.value} allRowsVisible theme="row-stripes" className="border rounded-xl overflow-hidden shadow-sm">
            <GridColumn header="Викладач" path="fullName" autoWidth />
            <GridColumn 
              header="Пріоритет" 
              autoWidth
              renderer={({ item }) => (
                <span className={`px-2 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider ${
                  item.priority === Priority.PRIMARY ? 'bg-green-100 text-green-700 border border-green-200' :
                  item.priority === Priority.SECONDARY ? 'bg-blue-100 text-blue-700 border border-blue-200' : 
                  'bg-gray-100 text-gray-600 border border-gray-200'
                }`}>
                  {translatePriority(item.priority)}
                </span>
              )}
            />
            <GridColumn 
              header="Пари сьогодні" 
              autoWidth
              renderer={({ item }) => (
                <div className="flex items-center gap-2">
                   <div className="w-8 h-1 bg-gray-100 rounded-full overflow-hidden">
                      <div className="h-full bg-blue-500" style={{ width: `${Math.min(item.currentWorkload * 25, 100)}%` }}></div>
                   </div>
                   <span className="text-xs font-bold text-gray-700">{item.currentWorkload}</span>
                </div>
              )}
            />
            <GridColumn
              header="Дія"
              autoWidth
              renderer={({ item }) => (
                <Button 
                  theme="primary small" 
                  onClick={() => item.id && handleSelect(item.id as any)}
                  className="shadow-sm"
                >
                  Призначити
                </Button>
              )}
            />
          </Grid>
        )}
      </VerticalLayout>
    </Dialog>
  );
};
