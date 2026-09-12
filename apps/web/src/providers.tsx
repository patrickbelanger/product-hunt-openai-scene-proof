import { MantineProvider, createTheme } from '@mantine/core';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useState, type PropsWithChildren } from 'react';

const theme = createTheme({
  primaryColor: 'teal',
  primaryShade: 4,
  defaultRadius: 'sm',
  fontFamily: 'Inter, Segoe UI, sans-serif',
  headings: { fontFamily: 'Inter, Segoe UI, sans-serif', fontWeight: '500' },
});

export function Providers({ children }: PropsWithChildren) {
  const [client] = useState(() => new QueryClient({
    defaultOptions: { queries: { retry: 1, staleTime: 15_000 } },
  }));
  return <QueryClientProvider client={client}>
    <MantineProvider theme={theme} forceColorScheme="dark">{children}</MantineProvider>
  </QueryClientProvider>;
}
