import { getResponsiveLayout } from '@/ui/responsive';

describe('responsive layout', () => {
  it.each([360, 375, 767])('uses phone padding at %ipx', (width) => {
    expect(getResponsiveLayout(width)).toEqual({ horizontalPadding: 16 });
  });

  it.each([768, 1023])('uses tablet padding at %ipx', (width) => {
    expect(getResponsiveLayout(width)).toEqual({ horizontalPadding: 24 });
  });

  it.each([1024, 1440])('uses desktop padding at %ipx', (width) => {
    expect(getResponsiveLayout(width)).toEqual({ horizontalPadding: 32 });
  });
});
