import { AppLayout, DrawerToggle, Icon, Scroller, SideNav, SideNavItem, Checkbox } from '@vaadin/react-components';
import '@vaadin/icons/vaadin-iconset.js';
import { Suspense, useEffect } from 'react';
import { Outlet, useLocation } from 'react-router';
import { useSignal } from '@vaadin/hilla-react-signals';
import { ScheduleEndpoint } from '../generated/endpoints';
import { BASE_TEMPLATE_LOCKED_MESSAGE, isBaseTemplateLocked, isPublished, refreshSchedule, solverStatus } from '../store/app-state';
import { Notification } from '@vaadin/react-components/Notification.js';
import { notifyDataChanged, useCrossTabRefresh } from '../utils/cross-tab-sync';

export default function MainLayout() {
  const userRole = useSignal<string | undefined>(undefined);
  const isDispatcher = userRole.value === 'DISPATCHER'; 
  const location = useLocation();
  const isSolving = solverStatus.value === 'SOLVING_ACTIVE' || solverStatus.value === 'SOLVING_SCHEDULED';

  useEffect(() => {
    const init = async () => {
      const navigationEntry = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming | undefined;
      if (navigationEntry?.type === 'reload') {
        await ScheduleEndpoint.resetBaseTemplateOnPageReload();
      }
      await refreshSchedule(); // This updates global isPublished and scheduleData
      userRole.value = await ScheduleEndpoint.getCurrentUserRole();
    };
    init().catch((err) => console.error('Failed to initialize layout:', err));
  }, []);

  useEffect(() => {
    window.dispatchEvent(new CustomEvent('side-nav-location-changed'));
  }, [location.pathname, location.search]);

  useEffect(() => {
    if (!isSolving) {
      return;
    }
    const interval = window.setInterval(() => {
      void refreshSchedule(false);
    }, 3000);
    return () => window.clearInterval(interval);
  }, [isSolving]);

  useCrossTabRefresh(() => refreshSchedule(false));

  const handleToggle = async () => {
    if (isBaseTemplateLocked.value) {
      Notification.show(BASE_TEMPLATE_LOCKED_MESSAGE, { theme: 'primary', position: 'bottom-end' });
      await refreshSchedule(false);
      return;
    }
    await ScheduleEndpoint.togglePublishedStatus();
    await refreshSchedule();
    notifyDataChanged('schedule');
  };

  return (
    <AppLayout primarySection="drawer">
      <div slot="navbar" className="flex items-center gap-4 px-4 w-full">
        <DrawerToggle aria-label="Перемкнути меню"></DrawerToggle>
        <h1 className="text-lg m-0">ASMS V3</h1>
        
        <div className="ml-auto flex items-center gap-4">
          <span className={`px-2 py-1 rounded text-sm font-bold`}
                style={{ 
                  backgroundColor: isPublished.value ? 'var(--aura-success-color)' : 'var(--aura-warning-color)',
                  color: 'white'
                }}>
            {isPublished.value ? 'ОПУБЛІКОВАНО' : 'ЧЕРНЕТКА'}
          </span>
          {isDispatcher && (
            <Checkbox 
              label="Опублікувати" 
              checked={isPublished.value} 
              onCheckedChanged={(e) => {
                if (e.detail.value !== isPublished.value) handleToggle();
              }} 
            />
          )}
        </div>
      </div>

      <Scroller slot="drawer" className="p-2">
        <SideNav>
          <SideNavItem path="/dashboard">
            <Icon icon="vaadin:calendar" slot="prefix" />
            Розклад
          </SideNavItem>
          <SideNavItem path="/teachers">
            <Icon icon="vaadin:user-card" slot="prefix" />
            Викладачі
          </SideNavItem>
          <SideNavItem path="/subjects">
            <Icon icon="vaadin:book" slot="prefix" />
            Дисципліни
          </SideNavItem>
          <SideNavItem path="/groups">
            <Icon icon="vaadin:users" slot="prefix" />
            Групи
          </SideNavItem>
          <SideNavItem path="/rooms">
            <Icon icon="vaadin:home" slot="prefix" />
            Аудиторії
          </SideNavItem>
          <SideNavItem path="/course-plans">
            <Icon icon="vaadin:list" slot="prefix" />
            Навчальні плани
          </SideNavItem>
        </SideNav>
      </Scroller>

      <Suspense fallback={<div>Завантаження...</div>}>
        <Outlet />
      </Suspense>
    </AppLayout>
  );
}
