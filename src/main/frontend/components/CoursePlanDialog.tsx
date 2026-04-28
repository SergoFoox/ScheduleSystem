import React, { useEffect, useState } from 'react';
import { Dialog } from '@vaadin/react-components/Dialog.js';
import { Grid } from '@vaadin/react-components/Grid.js';
import { GridColumn } from '@vaadin/react-components/GridColumn.js';
import { Button } from '@vaadin/react-components/Button.js';
import { Icon } from '@vaadin/react-components/Icon.js';
import { Notification } from '@vaadin/react-components/Notification.js';
import { VerticalLayout } from '@vaadin/react-components/VerticalLayout.js';
import { HorizontalLayout } from '@vaadin/react-components/HorizontalLayout.js';
import { Select } from '@vaadin/react-components/Select.js';
import { IntegerField } from '@vaadin/react-components/IntegerField.js';
import { FormLayout } from '@vaadin/react-components/FormLayout.js';
import { CoursePlanEndpoint, GroupEndpoint, SubjectEndpoint, TeacherEndpoint } from '../generated/endpoints';
import type CoursePlanDTO from '../generated/com/sergofoox/domain/ui/dto/CoursePlanDTO';
import type GroupDTO from '../generated/com/sergofoox/domain/ui/dto/GroupDTO';
import type TeacherDTO from '../generated/com/sergofoox/domain/ui/dto/TeacherDTO';
import RoomType from '../generated/com/sergofoox/domain/plan/RoomType';
import Periodicity from '../generated/com/sergofoox/domain/plan/Periodicity';
import { SubjectDialog } from './SubjectDialog';

interface CoursePlanDialogProps {
  opened: boolean;
  group?: GroupDTO;
  onClose: () => void;
}

export const CoursePlanDialog: React.FC<CoursePlanDialogProps> = ({ opened, group, onClose }) => {
  const [plans, setPlans] = useState<CoursePlanDTO[]>([]);
  const [groups, setGroups] = useState<GroupDTO[]>([]);
  const [subjects, setSubjects] = useState<any[]>([]);
  const [teachers, setTeachers] = useState<TeacherDTO[]>([]);
  const [loading, setLoading] = useState(false);
  const [copying, setCopying] = useState(false);
  const [savingEdit, setSavingEdit] = useState(false);
  const [isAdding, setIsAdding] = useState(false);
  const [subjectDialogOpen, setSubjectOpened] = useState(false);
  const [copySourceGroupId, setCopySourceGroupId] = useState<number | undefined>(undefined);
  const [editingPlan, setEditingPlan] = useState<CoursePlanDTO | undefined>(undefined);
  const [editPlanData, setEditPlanData] = useState<Partial<CoursePlanDTO>>({});
  
  const [newPlan, setNewPlan] = useState<any>({
    subjectId: undefined,
    teacherId: undefined,
    secondTeacherId: undefined,
    lectureHours: 16,
    practiceHours: 16,
    labHours: 0,
    periodicity: Periodicity.WEEKLY,
    requiredRoomType: RoomType.GENERAL_CLASSROOM
  });

  const fetchData = async () => {
    if (!group?.id) return;
    setLoading(true);
    try {
      const [pData, sData, tData, gData] = await Promise.all([
        CoursePlanEndpoint.getPlansByGroup(group.id as any),
        SubjectEndpoint.getAllSubjects(),
        TeacherEndpoint.getAllTeachers(),
        GroupEndpoint.getAllGroups()
      ]);
      setPlans((pData || []).filter(p => !!p) as CoursePlanDTO[]);
      setSubjects((sData || []).filter(s => !!s));
      setTeachers((tData || []).filter(t => !!t) as TeacherDTO[]);
      setGroups((gData || []).filter(g => !!g) as GroupDTO[]);
    } catch (err) {
      console.error(err);
      Notification.show('Помилка завантаження даних', { theme: 'error' });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (opened && group) {
      setCopySourceGroupId(undefined);
      setIsAdding(false);
      setEditingPlan(undefined);
      setEditPlanData({});
      fetchData();
    }
  }, [opened, group?.id]);

  const handleSaveNew = async () => {
    if (!newPlan.subjectId) {
      Notification.show('Оберіть дисципліну', { theme: 'error' });
      return;
    }
    if (!newPlan.teacherId) {
      Notification.show('Оберіть викладача', { theme: 'error' });
      return;
    }
    try {
      const lectureHours = Number(newPlan.lectureHours || 0);
      const practiceHours = Number(newPlan.practiceHours || 0);
      const labHours = Number(newPlan.labHours || 0);

      const planToSave = {
        ...newPlan,
        groupId: group?.id,
        totalHours: lectureHours + practiceHours + labHours,
        lectureSessionsPerWeek: lectureHours > 0 ? 1 : 0,
        practiceSessionsPerWeek: practiceHours > 0 ? 1 : 0,
        labSessionsPerWeek: labHours > 0 ? 1 : 0,
        lecturePeriodicity: newPlan.periodicity || Periodicity.WEEKLY,
        practicePeriodicity: newPlan.periodicity || Periodicity.WEEKLY,
        labPeriodicity: newPlan.periodicity || Periodicity.WEEKLY
      };
      await CoursePlanEndpoint.savePlan(planToSave as any);
      Notification.show('Дисципліну додано до плану', { theme: 'success' });
      setIsAdding(false);
      fetchData();
    } catch (err) {
      console.error(err);
      Notification.show('Помилка збереження', { theme: 'error' });
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('Видалити цей предмет з плану?')) return;
    try {
      await CoursePlanEndpoint.deletePlan(id as any);
      Notification.show('Предмет видалено', { theme: 'success' });
      fetchData();
    } catch (err) {
      console.error(err);
      Notification.show('Помилка видалення', { theme: 'error' });
    }
  };

  const handleStartEdit = (plan: CoursePlanDTO) => {
    setIsAdding(false);
    setEditingPlan(plan);
    setEditPlanData({
      teacherId: plan.teacherId,
      secondTeacherId: plan.secondTeacherId
    });
  };

  const handleSaveEdit = async () => {
    if (!editingPlan) return;
    if (!editPlanData.teacherId) {
      Notification.show('Оберіть викладача', { theme: 'error' });
      return;
    }

    setSavingEdit(true);
    try {
      await CoursePlanEndpoint.savePlan({
        ...editingPlan,
        teacherId: editPlanData.teacherId,
        secondTeacherId: editPlanData.secondTeacherId
      } as any);
      Notification.show('Викладача оновлено', { theme: 'success' });
      setEditingPlan(undefined);
      setEditPlanData({});
      await fetchData();
    } catch (err) {
      console.error(err);
      Notification.show('Помилка збереження викладача', { theme: 'error' });
    } finally {
      setSavingEdit(false);
    }
  };

  const handleCancelEdit = () => {
    setEditingPlan(undefined);
    setEditPlanData({});
  };

  const availableCopyGroups = groups.filter(g => g.id && g.id !== group?.id);

  const handleCopyPlans = async () => {
    if (!group?.id || !copySourceGroupId) {
      Notification.show('Оберіть групу для копіювання', { theme: 'error' });
      return;
    }

    const sourceGroup = groups.find(g => g.id === copySourceGroupId);
    const confirmed = window.confirm(
      `Скопіювати навчальний план з групи "${sourceGroup?.name || ''}" до "${group.name}"?`
    );
    if (!confirmed) return;

    setCopying(true);
    try {
      const copiedCount = await CoursePlanEndpoint.copyPlansFromGroup(copySourceGroupId as any, group.id as any);
      Notification.show(
        copiedCount > 0 ? `Скопійовано предметів: ${copiedCount}` : 'Нових предметів для копіювання немає',
        { theme: copiedCount > 0 ? 'success' : 'primary' }
      );
      await fetchData();
    } catch (err) {
      console.error(err);
      Notification.show('Помилка копіювання плану', { theme: 'error' });
    } finally {
      setCopying(false);
    }
  };

  return (
    <Dialog
      headerTitle={`Навчальний план: ${group?.name || ''}`}
      opened={opened}
      onOpenedChanged={(e) => !e.detail.value && onClose()}
    >
      <VerticalLayout theme="spacing" className="items-stretch" style={{ width: '850px', maxWidth: '100%' }}>
        
        {isAdding ? (
          <div className="p-4 border rounded-lg bg-blue-50/50 space-y-4 shadow-inner">
            <h4 className="font-bold text-blue-800 flex items-center gap-2">
              <Icon icon="vaadin:plus" className="w-4 h-4" />
              Додавання нової дисципліни
            </h4>
            <div className="flex gap-2 items-end">
              <Select
                label="Оберіть предмет"
                className="flex-1"
                items={subjects.map(s => ({ label: s.name, value: s.id?.toString() }))}
                value={newPlan.subjectId?.toString()}
                onValueChanged={(e) => {
                  const subjectId = e.detail.value ? parseInt(e.detail.value) : undefined;
                  setNewPlan({
                    ...newPlan,
                    subjectId
                  });
                }}
              />
              <Button 
                theme="icon secondary" 
                onClick={() => setSubjectOpened(true)}
                title="Створити новий предмет у довіднику"
              >
                <Icon icon="vaadin:plus" />
              </Button>
            </div>
            <FormLayout responsiveSteps={[{ minWidth: '0', columns: 2 }]}>
              <Select
                label="Викладач"
                items={teachers.map(t => ({ label: t.fullName, value: t.id?.toString() }))}
                value={newPlan.teacherId?.toString()}
                onValueChanged={(e) => setNewPlan({...newPlan, teacherId: e.detail.value ? parseInt(e.detail.value) : undefined})}
              />
              <Select
                label="Другий викладач"
                items={teachers
                  .filter(t => t.id !== newPlan.teacherId)
                  .map(t => ({ label: t.fullName, value: t.id?.toString() }))}
                value={newPlan.secondTeacherId?.toString()}
                onValueChanged={(e) => setNewPlan({...newPlan, secondTeacherId: e.detail.value ? parseInt(e.detail.value) : undefined})}
              />
              <Select
                label="Тип аудиторії"
                items={[
                  { label: 'Загальна', value: RoomType.GENERAL_CLASSROOM },
                  { label: 'Лекційна', value: RoomType.LECTURE_HALL },
                  { label: 'Лабораторія', value: RoomType.LABORATORY },
                  { label: 'Комп. клас', value: RoomType.COMPUTER_CLASS },
                ]}
                value={newPlan.requiredRoomType}
                onValueChanged={(e) => setNewPlan({...newPlan, requiredRoomType: e.detail.value})}
              />
              <Select 
                label="Період"
                items={[
                  { label: 'Щотижня', value: Periodicity.WEEKLY },
                  { label: 'Чисельник', value: Periodicity.ODD_WEEKS },
                  { label: 'Знаменник', value: Periodicity.EVEN_WEEKS },
                ]}
                value={newPlan.periodicity || Periodicity.WEEKLY}
                onValueChanged={e => setNewPlan({...newPlan, periodicity: e.detail.value})}
              />
              <IntegerField label="Год. Лекції" value={newPlan.lectureHours?.toString()} onValueChanged={e => setNewPlan({...newPlan, lectureHours: e.detail.value})} />
              <IntegerField label="Год. Прак." value={newPlan.practiceHours?.toString()} onValueChanged={e => setNewPlan({...newPlan, practiceHours: e.detail.value})} />
              <IntegerField label="Год. Лаб." value={newPlan.labHours?.toString()} onValueChanged={e => setNewPlan({...newPlan, labHours: e.detail.value})} />
            </FormLayout>
            <div className="flex justify-end gap-2 mt-2">
               <Button theme="success primary" onClick={handleSaveNew}>Додати в план</Button>
               <Button theme="tertiary" onClick={() => setIsAdding(false)}>Скасувати</Button>
            </div>
          </div>
        ) : editingPlan ? (
          <div className="p-4 border rounded-lg bg-gray-50 space-y-4 shadow-inner">
            <h4 className="font-bold text-gray-800 flex items-center gap-2">
              <Icon icon="vaadin:edit" className="w-4 h-4" />
              Редагування викладача: {editingPlan.subjectName}
            </h4>
            <FormLayout responsiveSteps={[{ minWidth: '0', columns: 2 }]}>
              <Select
                label="Викладач"
                items={teachers.map(t => ({ label: t.fullName, value: t.id?.toString() }))}
                value={editPlanData.teacherId?.toString()}
                onValueChanged={(e) => {
                  const teacherId = e.detail.value ? parseInt(e.detail.value, 10) : undefined;
                  setEditPlanData({
                    ...editPlanData,
                    teacherId,
                    secondTeacherId: editPlanData.secondTeacherId === teacherId ? undefined : editPlanData.secondTeacherId
                  });
                }}
                disabled={savingEdit}
              />
              <Select
                label="Другий викладач"
                items={[
                  { label: 'Без другого викладача', value: '' },
                  ...teachers
                    .filter(t => t.id !== editPlanData.teacherId)
                    .map(t => ({ label: t.fullName, value: t.id?.toString() }))
                ]}
                value={editPlanData.secondTeacherId?.toString() || ''}
                onValueChanged={(e) => setEditPlanData({...editPlanData, secondTeacherId: e.detail.value ? parseInt(e.detail.value, 10) : undefined})}
                disabled={savingEdit}
              />
            </FormLayout>
            <div className="flex justify-end gap-2 mt-2">
              <Button theme="primary" onClick={handleSaveEdit} disabled={savingEdit}>
                {savingEdit ? 'Збереження...' : 'Зберегти'}
              </Button>
              <Button theme="tertiary" onClick={handleCancelEdit} disabled={savingEdit}>Скасувати</Button>
            </div>
          </div>
        ) : (
          <div className="flex flex-col gap-3 bg-gray-50 p-3 rounded-lg border border-gray-200 shadow-sm">
            <div className="flex flex-wrap justify-between items-center gap-3">
              <span className="text-sm text-gray-600 font-medium">
                 Список дисциплін для поточної групи
              </span>
              <Button theme="primary" onClick={() => setIsAdding(true)}>
                <Icon icon="vaadin:plus" slot="prefix" />
                Додати предмет
              </Button>
            </div>
            <div className="flex flex-wrap items-end gap-2">
              <Select
                label="Група-джерело"
                className="min-w-[260px] flex-1"
                items={availableCopyGroups.map(g => ({ label: g.name, value: g.id?.toString() }))}
                value={copySourceGroupId?.toString()}
                onValueChanged={(e) => setCopySourceGroupId(e.detail.value ? parseInt(e.detail.value, 10) : undefined)}
                disabled={availableCopyGroups.length === 0 || copying || loading}
              />
              <Button
                theme="secondary"
                onClick={handleCopyPlans}
                disabled={!copySourceGroupId || copying || loading}
                title="Скопіювати план з вибраної групи"
              >
                <Icon icon="vaadin:copy" slot="prefix" />
                {copying ? 'Копіювання...' : 'Скопіювати план'}
              </Button>
            </div>
          </div>
        )}

        <Grid items={plans} className="border rounded-xl shadow-sm min-h-[350px] bg-white overflow-hidden" theme="row-stripes">
          <GridColumn header="Дисципліна" path="subjectName" autoWidth />
          <GridColumn
            header="Викладачі"
            autoWidth
            renderer={({ item }) => (
              <span>{[item.teacherName, item.secondTeacherName].filter(Boolean).join(', ')}</span>
            )}
          />
          <GridColumn header="Аудиторія" path="requiredRoomType" autoWidth />
          <GridColumn header="Лекції" path="lectureHours" autoWidth textAlign="center" />
          <GridColumn header="Практики" path="practiceHours" autoWidth textAlign="center" />
          <GridColumn header="Лаб." path="labHours" autoWidth textAlign="center" />
          <GridColumn header="Всього" path="totalHours" autoWidth textAlign="end" />
          <GridColumn
            header={<Icon icon="vaadin:cog" className="w-3 h-3" />}
            autoWidth
            renderer={({ item }) => (
              <div className="flex gap-2 p-1">
                <Button 
                  theme="tertiary icon" 
                  onClick={() => handleStartEdit(item as CoursePlanDTO)}
                  title="Редагувати викладача"
                >
                  <Icon icon="vaadin:edit" />
                </Button>
                <Button 
                  theme="tertiary error icon" 
                  onClick={() => item.id && handleDelete(item.id as any)}
                  title="Видалити"
                >
                  <Icon icon="vaadin:trash" />
                </Button>
              </div>
            )}
          />
        </Grid>

        <div className="flex justify-end w-full border-t pt-4">
          <Button theme="tertiary" onClick={onClose}>Закрити</Button>
        </div>
      </VerticalLayout>

      <SubjectDialog 
        opened={subjectDialogOpen} 
        onClose={() => setSubjectOpened(false)} 
        onSaved={(id) => {
          fetchData(); // Оновити список предметів
          setNewPlan({...newPlan, subjectId: id}); // Одразу обрати створений предмет
        }}
      />
    </Dialog>
  );
};
