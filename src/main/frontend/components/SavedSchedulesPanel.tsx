import React, { useEffect, useRef, useState } from 'react';
import { Button } from '@vaadin/react-components/Button.js';
import { Icon } from '@vaadin/react-components/Icon.js';
import { Notification } from '@vaadin/react-components/Notification.js';
import { TextField } from '@vaadin/react-components/TextField.js';
import { Dialog } from '@vaadin/react-components/Dialog.js';
import { AutosaveEndpoint, ScheduleEndpoint } from '../generated/endpoints';
import {
  activeSavedSchedule,
  BASE_TEMPLATE_LOCKED_MESSAGE,
  getMutationErrorMessage,
  isBaseTemplateLocked,
  isPublished,
  refreshSchedule
} from '../store/app-state';

type SavedSchedule = {
  id?: number;
  name?: string;
  createdAt?: string;
  updatedAt?: string;
  lessonCount?: number;
  isBuiltIn?: boolean;
  isFullTemplate?: boolean;
};

export const SavedSchedulesPanel: React.FC = () => {
  const [items, setItems] = useState<SavedSchedule[]>([]);
  const [name, setName] = useState('');
  const [loading, setLoading] = useState(false);
  const [collapsed, setCollapsed] = useState(false);
  const [copyDialogOpened, setCopyDialogOpened] = useState(false);
  const [copyName, setCopyName] = useState('');
  const [scheduleToCopy, setScheduleToCopy] = useState<SavedSchedule | undefined>(undefined);
  const [renameDialogOpened, setRenameDialogOpened] = useState(false);
  const [renameName, setRenameName] = useState('');
  const [scheduleToRename, setScheduleToRename] = useState<SavedSchedule | undefined>(undefined);
  const [draggingId, setDraggingId] = useState<number | undefined>(undefined);
  const [dragOverId, setDragOverId] = useState<number | undefined>(undefined);
  const suppressNextClickRef = useRef(false);
  const published = isPublished.value;
  const baseTemplateLocked = isBaseTemplateLocked.value;

  const isCustomSchedule = (schedule: SavedSchedule) => !!schedule.id && schedule.id > 0 && !schedule.isBuiltIn;

  const loadItems = async () => {
    const saved = await ScheduleEndpoint.getSavedSchedules();
    setItems((saved || []) as SavedSchedule[]);
  };

  useEffect(() => {
    loadItems().catch((err) => {
      console.error('Failed to load saved schedules:', err);
    });
  }, []);

  const handleSave = async () => {
    const trimmed = name.trim();
    if (!trimmed) {
      Notification.show('Вкажіть назву розкладу', { theme: 'error', position: 'bottom-end' });
      return;
    }

    setLoading(true);
    try {
      await ScheduleEndpoint.saveCurrentSchedule(trimmed);
      setName('');
      await loadItems();
      Notification.show('Порожній розклад створено', { theme: 'success', position: 'bottom-end' });
    } catch (err) {
      console.error('Failed to save schedule:', err);
      const message = err instanceof Error && err.message.includes('існує')
        ? 'Розклад із такою назвою вже існує'
        : getMutationErrorMessage(err, 'Помилка під час створення розкладу');
      Notification.show(message, { theme: 'error', position: 'bottom-end' });
    } finally {
      setLoading(false);
    }
  };

  const handleLoad = async (schedule: SavedSchedule) => {
    if (suppressNextClickRef.current) {
      suppressNextClickRef.current = false;
      return;
    }
    if (!schedule.id || published || loading) return;
    const confirmMessage = baseTemplateLocked
      ? `Відкрити "${schedule.name}"?`
      : `Якщо не зберегти поточні зміни, вони будуть втрачені. Відкрити "${schedule.name}"?`;
    if (!window.confirm(confirmMessage)) return;

    setLoading(true);
    try {
      await ScheduleEndpoint.loadSavedSchedule(schedule.id);
      await refreshSchedule();
      Notification.show(schedule.isBuiltIn ? 'Базовий шаблон відкрито для перегляду' : 'Розклад завантажено', { theme: 'success', position: 'bottom-end' });
    } catch (err) {
      console.error('Failed to load schedule:', err);
      Notification.show('Помилка під час завантаження розкладу', { theme: 'error', position: 'bottom-end' });
    } finally {
      setLoading(false);
    }
  };

  const openCopyDialog = (event: React.MouseEvent, schedule?: SavedSchedule) => {
    event.stopPropagation();
    setScheduleToCopy(schedule);
    setCopyName(schedule?.name ? `${schedule.name} копія` : '');
    setCopyDialogOpened(true);
  };

  const handleCopySchedule = async () => {
    const trimmed = copyName.trim();
    if (!trimmed) {
      Notification.show('Вкажіть назву копії', { theme: 'error', position: 'bottom-end' });
      return;
    }
    const customScheduleId = scheduleToCopy?.id && scheduleToCopy.id > 0 ? scheduleToCopy.id : undefined;
    if (!customScheduleId && !baseTemplateLocked && !window.confirm('Якщо не зберегти поточні зміни, вони будуть втрачені. Створити копію базового шаблону?')) {
      return;
    }

    setLoading(true);
    try {
      if (customScheduleId) {
        await ScheduleEndpoint.copySavedSchedule(customScheduleId, trimmed);
      } else {
        await ScheduleEndpoint.copyBuiltInTemplate(trimmed);
      }
      setCopyDialogOpened(false);
      setScheduleToCopy(undefined);
      setCopyName('');
      await loadItems();
      if (!customScheduleId) {
        await refreshSchedule();
      }
      Notification.show('Копію розкладу створено', { theme: 'success', position: 'bottom-end' });
    } catch (err) {
      console.error('Failed to copy schedule:', err);
      const message = err instanceof Error && err.message.includes('існує')
        ? 'Розклад із такою назвою вже існує'
        : 'Помилка під час копіювання розкладу';
      Notification.show(message, { theme: 'error', position: 'bottom-end' });
    } finally {
      setLoading(false);
    }
  };

  const openRenameDialog = (event: React.MouseEvent, schedule: SavedSchedule) => {
    event.stopPropagation();
    if (!schedule.id || schedule.id < 0) return;
    setScheduleToRename(schedule);
    setRenameName(schedule.name || '');
    setRenameDialogOpened(true);
  };

  const handleRename = async () => {
    if (!scheduleToRename?.id) return;

    const trimmed = renameName.trim();
    if (!trimmed) {
      Notification.show('Вкажіть нову назву', { theme: 'error', position: 'bottom-end' });
      return;
    }

    setLoading(true);
    try {
      await ScheduleEndpoint.renameSavedSchedule(scheduleToRename.id, trimmed);
      setRenameDialogOpened(false);
      setScheduleToRename(undefined);
      setRenameName('');
      await loadItems();
      Notification.show('Назву змінено', { theme: 'success', position: 'bottom-end' });
    } catch (err) {
      console.error('Failed to rename saved schedule:', err);
      const message = err instanceof Error && (err.message.includes('існує') || err.message.includes('зарезервовано'))
        ? err.message
        : 'Помилка під час перейменування';
      Notification.show(message, { theme: 'error', position: 'bottom-end' });
    } finally {
      setLoading(false);
    }
  };

  const handleSaveIntoSchedule = async (event: React.MouseEvent, schedule: SavedSchedule) => {
    event.stopPropagation();
    if (!schedule.id || schedule.id < 0 || loading || published) return;
    if (baseTemplateLocked) {
      Notification.show(BASE_TEMPLATE_LOCKED_MESSAGE, { theme: 'primary', position: 'bottom-end' });
      return;
    }
    if (!window.confirm(`Зберегти поточні зміни у "${schedule.name}"?`)) return;

    setLoading(true);
    try {
      await ScheduleEndpoint.saveCurrentScheduleToSavedSchedule(schedule.id);
      
      // Робимо ручний знімок у Машину Часу після успішного збереження
      try {
        await AutosaveEndpoint.captureManualSnapshot();
      } catch (e) {
        console.error('Failed to capture manual snapshot:', e);
      }
      
      await loadItems();
      Notification.show('Зміни збережено', { theme: 'success', position: 'bottom-end' });
    } catch (err) {
      console.error('Failed to save changes into saved schedule:', err);
      Notification.show(getMutationErrorMessage(err, 'Помилка під час збереження змін'), { theme: 'error', position: 'bottom-end' });
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (event: React.MouseEvent, schedule: SavedSchedule) => {
    event.stopPropagation();
    if (!schedule.id || schedule.id < 0) return;
    if (!window.confirm(`Видалити "${schedule.name}"?`)) return;

    setLoading(true);
    try {
      await ScheduleEndpoint.deleteSavedSchedule(schedule.id);
      await loadItems();
      await refreshSchedule();
      Notification.show('Збережений розклад видалено', { theme: 'success', position: 'bottom-end' });
    } catch (err) {
      console.error('Failed to delete saved schedule:', err);
      Notification.show('Помилка під час видалення розкладу', { theme: 'error', position: 'bottom-end' });
    } finally {
      setLoading(false);
    }
  };

  const handleDragStart = (event: React.DragEvent, schedule: SavedSchedule) => {
    if (!isCustomSchedule(schedule) || loading || published) {
      event.preventDefault();
      return;
    }
    suppressNextClickRef.current = true;
    setDraggingId(schedule.id);
    event.dataTransfer.effectAllowed = 'move';
    event.dataTransfer.setData('text/plain', String(schedule.id));
  };

  const handleDragOver = (event: React.DragEvent, schedule: SavedSchedule) => {
    if (!isCustomSchedule(schedule) || !draggingId || draggingId === schedule.id) return;
    event.preventDefault();
    event.dataTransfer.dropEffect = 'move';
    setDragOverId(schedule.id);
  };

  const handleDrop = async (event: React.DragEvent, schedule: SavedSchedule) => {
    event.preventDefault();
    event.stopPropagation();
    const sourceId = Number(event.dataTransfer.getData('text/plain') || draggingId);
    const targetId = schedule.id;
    suppressNextClickRef.current = true;
    setDraggingId(undefined);
    setDragOverId(undefined);

    if (!sourceId || !targetId || sourceId === targetId || !isCustomSchedule(schedule)) return;

    const previousItems = items;
    const customItems = previousItems.filter(isCustomSchedule);
    const sourceIndex = customItems.findIndex((item) => item.id === sourceId);
    const targetIndex = customItems.findIndex((item) => item.id === targetId);
    if (sourceIndex < 0 || targetIndex < 0) return;

    const reorderedCustomItems = [...customItems];
    const [movedItem] = reorderedCustomItems.splice(sourceIndex, 1);
    reorderedCustomItems.splice(targetIndex, 0, movedItem);
    const pinnedItems = previousItems.filter((item) => !isCustomSchedule(item));
    setItems([...pinnedItems, ...reorderedCustomItems]);

    setLoading(true);
    try {
      await ScheduleEndpoint.reorderSavedSchedules(reorderedCustomItems.map((item) => item.id as number));
    } catch (err) {
      console.error('Failed to reorder saved schedules:', err);
      setItems(previousItems);
      Notification.show('Помилка під час зміни порядку розкладів', { theme: 'error', position: 'bottom-end' });
    } finally {
      setLoading(false);
    }
  };

  const handleDragEnd = () => {
    setDraggingId(undefined);
    setDragOverId(undefined);
    window.setTimeout(() => {
      suppressNextClickRef.current = false;
    }, 150);
  };

  if (collapsed) {
    return (
      <aside className="w-10 shrink-0 border-r border-gray-200 bg-white flex items-center justify-center">
        <button
          type="button"
          title="Показати збережені розклади"
          onClick={() => setCollapsed(false)}
          className="flex h-9 w-7 items-center justify-center rounded-r-md border border-l-0 border-gray-300 bg-white text-gray-600 shadow-sm transition-colors hover:border-gray-500 hover:bg-gray-50 hover:text-gray-900"
        >
          <Icon icon="vaadin:angle-right" className="h-4 w-4" />
        </button>
      </aside>
    );
  }

  return (
    <aside className="relative w-64 shrink-0 border-r border-gray-200 bg-white flex flex-col min-h-0">
      <button
        type="button"
        title="Сховати збережені розклади"
        onClick={() => setCollapsed(true)}
        className="absolute -right-3 top-1/2 z-20 flex h-9 w-6 -translate-y-1/2 items-center justify-center rounded-r-md border border-l-0 border-gray-300 bg-white text-gray-600 shadow-sm transition-colors hover:border-gray-500 hover:bg-gray-50 hover:text-gray-900"
      >
        <Icon icon="vaadin:angle-left" className="h-4 w-4" />
      </button>

      <div className="border-b border-gray-200 px-4 py-3">
        <div className="flex items-center gap-2 text-sm font-bold uppercase tracking-wide text-gray-800">
          <Icon icon="vaadin:calendar-clock" className="h-4 w-4 text-gray-500" />
          Збережені розклади
        </div>
      </div>

      <div className="border-b border-gray-200 p-3 space-y-2">
        <TextField
          value={name}
          placeholder="Назва розкладу"
          clearButtonVisible
          className="w-full"
          disabled={loading}
          onValueChanged={(event) => setName(event.detail.value)}
          onKeyDown={(event) => {
            if (event.key === 'Enter') {
              handleSave();
            }
          }}
        />
        <Button theme="primary" className="w-full" onClick={handleSave} disabled={loading}>
          <Icon icon="vaadin:archive" slot="prefix" />
          Зберегти поточний
        </Button>
      </div>

      <div className="flex-1 overflow-auto p-2">
        {items.length === 0 ? (
          <div className="px-3 py-6 text-center text-sm text-gray-500">
            Немає збережених розкладів
          </div>
        ) : (
          <div className="space-y-1">
            {items.map((schedule) => {
              const isActive = activeSavedSchedule.value?.id === schedule.id;
              
              return (
                <div
                  key={schedule.id}
                  role="button"
                  tabIndex={loading || published ? -1 : 0}
                  title={isActive ? "Поточний розклад" : "Натисніть, щоб відкрити"}
                  draggable={isCustomSchedule(schedule) && !loading && !published}
                  onDragStart={(event) => handleDragStart(event, schedule)}
                  onDragOver={(event) => handleDragOver(event, schedule)}
                  onDragLeave={() => {
                    if (dragOverId === schedule.id) {
                      setDragOverId(undefined);
                    }
                  }}
                  onDrop={(event) => handleDrop(event, schedule)}
                  onDragEnd={handleDragEnd}
                  onClick={() => handleLoad(schedule)}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter') {
                      handleLoad(schedule);
                    }
                  }}
                  className={`group w-full border px-3 py-2 text-left transition-all ${
                    isActive 
                      ? 'border-black ring-1 ring-black bg-gray-50 shadow-sm z-10' 
                      : dragOverId === schedule.id
                        ? 'border-gray-900 bg-gray-100'
                        : schedule.isBuiltIn && baseTemplateLocked
                          ? 'border-gray-900 bg-gray-50'
                          : 'border-gray-200 bg-white hover:border-gray-400 hover:bg-gray-50'
                  } ${loading || published ? 'cursor-not-allowed opacity-50' : 'cursor-pointer'
                  } ${draggingId === schedule.id ? 'opacity-40' : ''
                  } ${isCustomSchedule(schedule) && !loading && !published ? 'cursor-grab active:cursor-grabbing' : ''
                  }`}
                >
                  <div className="flex items-start justify-between gap-2">
                    <div className="min-w-0">
                      <div className="truncate text-sm font-semibold text-gray-900">{schedule.name}</div>
                      <div className="mt-0.5 text-[11px] text-gray-500">
                        {schedule.updatedAt || '—'} · {schedule.lessonCount ?? 0} занять
                      </div>
                    </div>
                    {schedule.isBuiltIn && (
                      <Button
                        theme="tertiary-inline small"
                        title="Скопіювати базовий шаблон"
                        className="opacity-100"
                        onClick={openCopyDialog}
                      >
                        <Icon icon="vaadin:copy" className="h-3.5 w-3.5" />
                      </Button>
                    )}
                    {schedule.id && schedule.id > 0 && (
                      <div className="flex shrink-0 items-center gap-1">
                        <Button
                          theme="tertiary-inline small"
                          title="Зберегти зміни"
                          disabled={loading || published}
                          onClick={(event) => handleSaveIntoSchedule(event, schedule)}
                        >
                          <Icon icon="vaadin:archive" className="h-3.5 w-3.5" />
                        </Button>
                        <Button
                          theme="tertiary-inline small"
                          title="Скопіювати"
                          disabled={loading}
                          onClick={(event) => openCopyDialog(event, schedule)}
                        >
                          <Icon icon="vaadin:copy" className="h-3.5 w-3.5" />
                        </Button>
                        <Button
                          theme="tertiary-inline small"
                          title="Перейменувати"
                          onClick={(event) => openRenameDialog(event, schedule)}
                        >
                          <Icon icon="vaadin:edit" className="h-3.5 w-3.5" />
                        </Button>
                        <Button
                          theme="tertiary-inline small error"
                          title="Видалити"
                          onClick={(event) => handleDelete(event, schedule)}
                        >
                          <Icon icon="vaadin:trash" className="h-3.5 w-3.5" />
                        </Button>
                      </div>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      <Dialog
        headerTitle={scheduleToCopy ? 'Копіювати розклад' : 'Копіювати шаблон'}
        opened={copyDialogOpened}
        onOpenedChanged={(event) => {
          if (!event.detail.value) {
            setCopyDialogOpened(false);
            setScheduleToCopy(undefined);
          }
        }}
        footerRenderer={() => (
          <div className="flex justify-end gap-2 p-2">
            <Button theme="primary" onClick={handleCopySchedule} disabled={loading}>
              ОК
            </Button>
            <Button
              onClick={() => {
                setCopyDialogOpened(false);
                setScheduleToCopy(undefined);
              }}
              disabled={loading}
            >
              Скасувати
            </Button>
          </div>
        )}
      >
        <div className="w-80 max-w-full p-4">
          <TextField
            label="Назва копії"
            value={copyName}
            className="w-full"
            autofocus
            disabled={loading}
            onValueChanged={(event) => setCopyName(event.detail.value)}
            onKeyDown={(event) => {
              if (event.key === 'Enter') {
                handleCopySchedule();
              }
            }}
          />
        </div>
      </Dialog>

      <Dialog
        headerTitle="Перейменувати розклад"
        opened={renameDialogOpened}
        onOpenedChanged={(event) => {
          if (!event.detail.value) {
            setRenameDialogOpened(false);
            setScheduleToRename(undefined);
          }
        }}
        footerRenderer={() => (
          <div className="flex justify-end gap-2 p-2">
            <Button theme="primary" onClick={handleRename} disabled={loading}>
              ОК
            </Button>
            <Button
              onClick={() => {
                setRenameDialogOpened(false);
                setScheduleToRename(undefined);
              }}
              disabled={loading}
            >
              Скасувати
            </Button>
          </div>
        )}
      >
        <div className="w-80 max-w-full p-4">
          <TextField
            label="Нова назва"
            value={renameName}
            className="w-full"
            autofocus
            disabled={loading}
            onValueChanged={(event) => setRenameName(event.detail.value)}
            onKeyDown={(event) => {
              if (event.key === 'Enter') {
                handleRename();
              }
            }}
          />
        </div>
      </Dialog>
    </aside>
  );
};
