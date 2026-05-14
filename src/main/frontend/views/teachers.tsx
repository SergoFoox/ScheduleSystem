import React, { useEffect, useState } from 'react';
import { Grid } from '@vaadin/react-components/Grid.js';
import { GridColumn } from '@vaadin/react-components/GridColumn.js';
import { TextField } from '@vaadin/react-components/TextField.js';
import { Button } from '@vaadin/react-components/Button.js';
import { Icon } from '@vaadin/react-components/Icon.js';
import { Notification } from '@vaadin/react-components/Notification.js';
import { ConfirmDialog } from '@vaadin/react-components/ConfirmDialog.js';
import { Tabs } from '@vaadin/react-components/Tabs.js';
import { Tab } from '@vaadin/react-components/Tab.js';
import { TeacherEndpoint } from '../generated/endpoints';
import type TeacherDTO from '../generated/com/sergofoox/domain/ui/dto/TeacherDTO';
import { TeacherDialog } from '../components/TeacherDialog';
import { CompetenceDialog } from '../components/CompetenceDialog';
import { AvailabilityDialog } from '../components/AvailabilityDialog';
import { useSignal } from '@vaadin/hilla-react-signals';
import { formatPositionType } from '../utils/labels';
import { BASE_TEMPLATE_LOCKED_MESSAGE, getMutationErrorMessage, isBaseTemplateLocked } from '../store/app-state';
import { notifyDataChanged, useCrossTabRefresh } from '../utils/cross-tab-sync';

export default function TeachersView() {
  const [teachers, setTeachers] = useState<TeacherDTO[]>([]);
  const [filter, setFilter] = useState('');
  const [dialogOpened, setDialogOpened] = useState(false);
  const [competenceOpened, setCompetenceOpened] = useState(false);
  const [availabilityOpened, setAvailabilityOpened] = useState(false);
  const [confirmOpened, setConfirmOpened] = useState(false);
  const [selectedTeacher, setSelectedTeacher] = useState<TeacherDTO | undefined>(undefined);
  const [teacherToDelete, setTeacherToDelete] = useState<number | undefined>(undefined);
  const [selectedTab, setSelectedTab] = useState(0);
  const loading = useSignal(true);

  const fetchTeachers = async (showLoading = true) => {
    if (showLoading) {
      loading.value = true;
    }
    try {
      const data = await TeacherEndpoint.getAllTeachers();
      setTeachers((data || []).filter(t => !!t) as TeacherDTO[]);
    } catch (err) {
      console.error('Failed to fetch teachers:', err);
      Notification.show('Помилка завантаження списку викладачів', { theme: 'error', position: 'bottom-end' });
    } finally {
      if (showLoading) {
        loading.value = false;
      }
    }
  };

  useEffect(() => {
    fetchTeachers();
  }, []);

  useEffect(() => {
    const refreshIfVisible = () => {
      if (document.visibilityState !== 'hidden') {
        void fetchTeachers(false);
      }
    };
    const interval = window.setInterval(refreshIfVisible, 5000);
    window.addEventListener('focus', refreshIfVisible);
    document.addEventListener('visibilitychange', refreshIfVisible);
    return () => {
      window.clearInterval(interval);
      window.removeEventListener('focus', refreshIfVisible);
      document.removeEventListener('visibilitychange', refreshIfVisible);
    };
  }, []);

  useCrossTabRefresh(() => fetchTeachers(false));

  const filteredTeachers = teachers.filter(teacher => {
    const matchesFilter = 
      teacher.fullName?.toLowerCase().includes(filter.toLowerCase()) ||
      teacher.department?.toLowerCase().includes(filter.toLowerCase());
    const matchesTab = selectedTab === 0 ? !teacher.archived : teacher.archived;
    return matchesFilter && matchesTab;
  });

  const handleAdd = () => {
    if (isBaseTemplateLocked.value) {
      Notification.show(BASE_TEMPLATE_LOCKED_MESSAGE, { theme: 'primary', position: 'bottom-end' });
      return;
    }
    setSelectedTeacher(undefined);
    setDialogOpened(true);
  };

  const handleEdit = (teacher: TeacherDTO) => {
    if (isBaseTemplateLocked.value) {
      Notification.show(BASE_TEMPLATE_LOCKED_MESSAGE, { theme: 'primary', position: 'bottom-end' });
      return;
    }
    setSelectedTeacher(teacher);
    setDialogOpened(true);
  };

  const handleCompetence = (teacher: TeacherDTO) => {
    if (isBaseTemplateLocked.value) {
      Notification.show(BASE_TEMPLATE_LOCKED_MESSAGE, { theme: 'primary', position: 'bottom-end' });
      return;
    }
    setSelectedTeacher(teacher);
    setCompetenceOpened(true);
  };

  const handleAvailability = (teacher: TeacherDTO) => {
    if (isBaseTemplateLocked.value) {
      Notification.show(BASE_TEMPLATE_LOCKED_MESSAGE, { theme: 'primary', position: 'bottom-end' });
      return;
    }
    setSelectedTeacher(teacher);
    setAvailabilityOpened(true);
  };

  const openDeleteConfirm = (id: number) => {
    if (isBaseTemplateLocked.value) {
      Notification.show(BASE_TEMPLATE_LOCKED_MESSAGE, { theme: 'primary', position: 'bottom-end' });
      return;
    }
    setTeacherToDelete(id);
    setConfirmOpened(true);
  };

  const handleDelete = async () => {
    if (teacherToDelete === undefined) return;
    try {
      await TeacherEndpoint.deleteTeacher(teacherToDelete as any);
      Notification.show('Викладача видалено', { theme: 'success', position: 'bottom-end' });
      setConfirmOpened(false);
      await fetchTeachers();
      notifyDataChanged('teachers');
    } catch (err) {
      console.error('Failed to delete teacher:', err);
      Notification.show(getMutationErrorMessage(err, 'Помилка під час видалення'), { theme: 'error', position: 'bottom-end' });
    }
  };

  const handleRestore = async (id: number) => {
    if (isBaseTemplateLocked.value) {
      Notification.show(BASE_TEMPLATE_LOCKED_MESSAGE, { theme: 'primary', position: 'bottom-end' });
      return;
    }
    try {
      await TeacherEndpoint.restoreTeacher(id as any);
      Notification.show('Викладача відновлено', { theme: 'success', position: 'bottom-end' });
      await fetchTeachers();
      notifyDataChanged('teachers');
    } catch (err) {
      console.error('Failed to restore teacher:', err);
      Notification.show(getMutationErrorMessage(err, 'Помилка під час відновлення'), { theme: 'error', position: 'bottom-end' });
    }
  };

  return (
    <div className="flex-1 flex flex-col gap-6 p-6 overflow-hidden bg-gray-50/50">
      <div className="flex justify-between items-center p-4 rounded-xl bg-white border shadow-sm">
        <div className="flex items-center gap-6 flex-1">
          <h2 className="text-2xl font-extrabold tracking-tight text-gray-900">
            Список викладачів
          </h2>
          <Tabs 
            selected={selectedTab} 
            onSelectedChanged={e => setSelectedTab(e.detail.value)}
          >
            <Tab>Активні</Tab>
            <Tab>Архів</Tab>
          </Tabs>
          <TextField
            placeholder="Пошук за ПІБ або кафедрою..."
            value={filter}
            onValueChanged={(e) => setFilter(e.detail.value)}
            className="w-96"
            clearButtonVisible
          >
            <Icon icon="vaadin:search" slot="prefix" className="text-gray-400" />
          </TextField>
        </div>
        {selectedTab === 0 && (
          <Button theme="primary" onClick={handleAdd} className="shadow-md">
            <Icon icon="vaadin:plus" slot="prefix" />
            Додати викладача
          </Button>
        )}
      </div>

      <div className="flex-1 overflow-hidden border rounded-xl shadow-lg bg-white">
        <Grid 
          items={filteredTeachers} 
          className="h-full" 
          theme="row-stripes"
        >
          <GridColumn header="ПІБ викладача" path="fullName" autoWidth />
          <GridColumn header="Спеціалізація" path="specialization" autoWidth />
          <GridColumn header="Кафедра" path="department" autoWidth />
          <GridColumn header="Аудиторія" path="assignedRoomName" autoWidth />
          <GridColumn 
            header="Посада" 
            autoWidth 
            renderer={({ item }) => (
              <span className="px-2 py-1 rounded-full text-xs font-medium bg-purple-50 text-purple-700 border border-purple-100">
                {formatPositionType((item as TeacherDTO).positionType)}
              </span>
            )}
          />
          <GridColumn header="Ліміт год/тиждень" path="weeklyHourLimit" autoWidth textAlign="end" />
          <GridColumn header="Макс. робочих днів" path="maxWorkingDaysPerWeek" autoWidth textAlign="end" />
          <GridColumn
            header={
              <div className="flex items-center gap-2">
                <Icon icon="vaadin:cog" className="w-3 h-3" />
                <span>Дії</span>
              </div>
            }
            autoWidth
            frozenToEnd
            renderer={({ item }) => {
              const teacher = item as TeacherDTO;
              return (
                <div className="flex gap-2 p-1">
                  <Button 
                    theme="tertiary icon" 
                    onClick={() => handleEdit(teacher)}
                    title="Редагувати"
                  >
                    <Icon icon="vaadin:edit" />
                  </Button>
                  <Button 
                    theme="tertiary icon" 
                    onClick={() => handleCompetence(teacher)}
                    title="Компетенції (дисципліни)"
                  >
                    <Icon icon="vaadin:book" />
                  </Button>
                  <Button
                    theme="tertiary icon"
                    onClick={() => handleAvailability(teacher)}
                    title="Преференції часу"
                  >
                    <Icon icon="vaadin:clock" />
                  </Button>
                  {!teacher.archived ? (
                    <Button 
                      theme="tertiary error icon" 
                      onClick={() => {
                        if (teacher.id) openDeleteConfirm(teacher.id as any);
                      }}
                      title="Видалити"
                    >
                      <Icon icon="vaadin:trash" />
                    </Button>
                  ) : (
                    <Button 
                      theme="tertiary icon" 
                      onClick={() => {
                        if (teacher.id) handleRestore(teacher.id as any);
                      }}
                      title="Відновити"
                    >
                      <Icon icon="vaadin:rotate-left" />
                    </Button>
                  )}
                </div>
              );
            }}
          />
        </Grid>
      </div>

      {dialogOpened && (
        <TeacherDialog 
          opened={dialogOpened} 
          teacher={selectedTeacher} 
          onClose={() => {
            setDialogOpened(false);
            setSelectedTeacher(undefined);
          }}
          onSaved={fetchTeachers}
        />
      )}

      <CompetenceDialog
        opened={competenceOpened}
        teacher={selectedTeacher}
        onClose={() => setCompetenceOpened(false)}
      />

      <AvailabilityDialog
        opened={availabilityOpened}
        teacher={selectedTeacher}
        onClose={() => {
          setAvailabilityOpened(false);
          setSelectedTeacher(undefined);
        }}
      />

      <ConfirmDialog
        header="Видалення викладача"
        cancelButtonVisible
        confirmText="Видалити"
        cancelText="Скасувати"
        confirmTheme="error primary"
        opened={confirmOpened}
        onOpenedChanged={(e) => setConfirmOpened(e.detail.value)}
        onConfirm={handleDelete}
      >
        Ви впевнені, що хочете видалити цього викладача? Це призведе до видалення його з усіх призначених занять.
      </ConfirmDialog>
    </div>
  );
}
