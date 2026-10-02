import { getNavigationMode, NAVIGATION_MODES } from '@/ui/navigation-mode';

describe('navigation mode by width', () => {
  it.each([320, 360, 375, 767])('uses bottom navigation at %ipx', (width) => {
    expect(getNavigationMode(width)).toBe('bottom');
  });

  it.each([768, 900, 1023])('uses the navigation rail at %ipx', (width) => {
    expect(getNavigationMode(width)).toBe('rail');
  });

  it.each([1024, 1280, 1440])('uses the sidebar at %ipx', (width) => {
    expect(getNavigationMode(width)).toBe('sidebar');
  });

  it('switches exactly at the approved boundaries, not one pixel either side', () => {
    expect(getNavigationMode(767)).toBe('bottom');
    expect(getNavigationMode(768)).toBe('rail');
    expect(getNavigationMode(1023)).toBe('rail');
    expect(getNavigationMode(1024)).toBe('sidebar');
  });

  it('declares the three modes the contract allows and no others', () => {
    expect([...NAVIGATION_MODES]).toEqual(['bottom', 'rail', 'sidebar']);
  });
});
