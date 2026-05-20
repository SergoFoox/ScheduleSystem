import { AppLayout, DrawerToggle, Icon, Scroller, SideNav, SideNavItem } from '@vaadin/react-components';
import '@vaadin/icons/vaadin-iconset.js';
import { Suspense, useEffect } from 'react';
import { Outlet, useLocation } from 'react-router';
import { ScheduleEndpoint } from '../generated/endpoints';
import { refreshSchedule, solverStatus } from '../store/app-state';
import { useCrossTabRefresh } from '../utils/cross-tab-sync';

export default function MainLayout() {
  const location = useLocation();
  const isSolving = solverStatus.value === 'SOLVING_ACTIVE' || solverStatus.value === 'SOLVING_SCHEDULED';

  useEffect(() => {
    const init = async () => {
      const navigationEntry = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming | undefined;
      if (navigationEntry?.type === 'reload') {
        await ScheduleEndpoint.resetBaseTemplateOnPageReload();
      }
      await refreshSchedule();
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

  return (
    <AppLayout primarySection="drawer">
      <div slot="navbar" className="flex items-center gap-4 px-4 w-full">
        <DrawerToggle aria-label="Перемкнути меню"></DrawerToggle>
        <h1 className="text-lg m-0">ASMS V3</h1>
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
