import React, { useEffect, useState } from 'react';
import { Dialog } from '@vaadin/react-components/Dialog.js';
import { Grid } from '@vaadin/react-components/Grid.js';
import { GridColumn } from '@vaadin/react-components/GridColumn.js';
import { Button } from '@vaadin/react-components/Button.js';
import { Select } from '@vaadin/react-components/Select.js';
import { Notification } from '@vaadin/react-components/Notification.js';
import { VerticalLayout, HorizontalLayout, FormLayout, Icon } from '@vaadin/react-components';
import { ScheduleEndpoint, RoomEndpoint, TeacherEndpoint } from '../generated/endpoints';
import type ReplacementCandidateDTO from '../generated/com/sergofoox/domain/ui/dto/ReplacementCandidateDTO';
import type LessonDTO from '../generated/com/sergofoox/domain/ui/dto/LessonDTO';
import type RoomDTO from '../generated/com/sergofoox/domain/ui/dto/RoomDTO';
import type TeacherDTO from '../generated/com/sergofoox/domain/ui/dto/TeacherDTO';
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
  const allTeachers = useSignal<TeacherDTO[]>([]);
  
  const [selectedRoomId, setSelectedRoomId] = useState<string | undefined>(undefined);
  const [selectedTeacherId, setSelectedTeacherId] = useState<string | undefined>(undefined);
  
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (opened && lesson.id) {
      setLoading(true);
      setSelectedRoomId(lesson.roomId?.toString());
      setSelectedTeacherId(lesson.teacherId?.toString());
      
      Promise.all([
        ScheduleEndpoint.getReplacementCandidates(lesson.id as any),
        RoomEndpoint.getAllRooms(),
        TeacherEndpoint.getAllTeachers()
      ]).then(([candidateData, roomData, teacherData]) => {
        candidates.value = candidateData;
        rooms.value = (roomData || []).filter(r => !!r) as RoomDTO[];
        allTeachers.value = (teacherData || []).filter(t => !!t) as TeacherDTO[];
      }).catch(err => {
        console.error('Failed to load data:', err);
      }).finally(() => {
        setLoading(false);
      });
    }
  }, [opened, lesson.id]);

  const handleApplyChanges = async () => {
    if (!lesson.id || !selectedTeacherId) return;
    setSaving(true);
    try {
      await ScheduleEndpoint.assignReplacement(
        lesson.id as any, 
        parseInt(selectedTeacherId) as any, 
        selectedRoomId ? parseInt(selectedRoomId) as any : undefined
      );
      await refreshSchedule();
      onClose();
    } catch (err) {
      console.error('Failed to assign replacement:', err);
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteLesson = async () => {
    if (!lesson.id) return;
    if (!confirm('Ви впевнені, що хочете видалити це заняття з розкладу? воно знову стане нерозподіленим.')) return;
    
    setSaving(true);
    try {
      await ScheduleEndpoint.unassignLesson(lesson.id as any);
      Notification.show('Заняття прибрано з розкладу', { theme: 'success' });
      await refreshSchedule();
      onClose();
    } catch (err) {
      console.error('Failed to unassign lesson:', err);
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog
      headerTitle="Коригування заняття"
      opened={opened}
      onOpenedChanged={(e) => !e.detail.value && onClose()}
      footerRenderer={() => (
        <HorizontalLayout theme="spacing" className="px-4 py-2 w-full justify-between items-center">
          <Button theme="error tertiary" onClick={handleDeleteLesson} disabled={saving || loading}>
             <Icon icon="vaadin:trash" slot="prefix" />
             Видалити заняття
          </Button>
          <HorizontalLayout theme="spacing">
            <Button theme="primary" onClick={handleApplyChanges} disabled={saving || loading}>
               <Icon icon="vaadin:check" slot="prefix" />
               Зберегти зміни
            </Button>
            <Button onClick={onClose}>Скасувати</Button>
          </HorizontalLayout>
        </HorizontalLayout>
      )}
    >
      <VerticalLayout style={{ alignItems: 'stretch', width: '800px', maxWidth: '100%' }} className="gap-m">
        {/* Секція 1: Основна інформація */}
        <div className="p-4 rounded-xl bg-gray-50 border border-gray-200 flex flex-col gap-2">
            <div className="flex justify-between items-center">
                <span className="text-2xl font-black text-black uppercase underline decoration-2">{lesson.subjectName}</span>
                <span className="px-3 py-1 bg-black text-white font-bold rounded-lg text-sm">{lesson.groupName}</span>
            </div>
            <div className="text-gray-600 font-medium">Поточне призначення: {lesson.teacherName} • {lesson.roomName ? `ауд.№${lesson.roomName}` : 'Без аудиторії'}</div>
        </div>

        {/* Секція 2: Форма вибору */}
        <div className="p-6 border-2 border-blue-100 rounded-2xl bg-blue-50/30">
            <h3 className="text-blue-900 font-bold mb-4 flex items-center gap-2">
                <Icon icon="vaadin:edit" className="w-4 h-4" />
                Параметри заміни
            </h3>
            <FormLayout responsiveSteps={[{ minWidth: '0', columns: 2 }]}>
                <Select
                    label="Викладач"
                    items={allTeachers.value.map(t => ({ label: t.fullName, value: t.id?.toString() }))}
                    value={selectedTeacherId}
                    onValueChanged={e => setSelectedTeacherId(e.detail.value)}
                    className="w-full"
                />
                <Select
                    label="Аудиторія"
                    items={rooms.value.map(r => ({ label: `${r.name} (${r.type})`, value: r.id?.toString() }))}
                    value={selectedRoomId}
                    onValueChanged={e => setSelectedRoomId(e.detail.value)}
                    className="w-full"
                />
            </FormLayout>
        </div>

        {/* Секція 3: Рекомендовані кандидати */}
        <div className="flex flex-col gap-2">
            <h3 className="text-gray-700 font-bold flex items-center gap-2 px-1">
                <Icon icon="vaadin:magic" className="w-4 h-4 text-purple-600" />
                Підбір вільних викладачів (автоматично)
            </h3>
            {loading ? (
                <div className="p-8 text-center text-gray-500 animate-pulse bg-white border rounded-xl">Пошук вільних колег...</div>
            ) : candidates.value.length === 0 ? (
                <div className="p-8 text-center text-gray-400 border-2 border-dashed rounded-xl bg-white">
                    Немає доступних викладачів для заміни на цей час
                </div>
            ) : (
                <Grid items={candidates.value} allRowsVisible theme="row-stripes" className="border rounded-xl overflow-hidden shadow-sm bg-white">
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
                            <span className="text-xs font-bold text-gray-700">{item.currentWorkload} занять</span>
                        )}
                    />
                    <GridColumn
                        header="Дія"
                        autoWidth
                        renderer={({ item }) => (
                            <Button 
                                theme="tertiary small" 
                                onClick={() => item.id && setSelectedTeacherId(item.id.toString())}
                                className="text-blue-600 font-bold"
                            >
                                Вибрати
                            </Button>
                        )}
                    />
                </Grid>
            )}
        </div>
      </VerticalLayout>
    </Dialog>
  );
};
