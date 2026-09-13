import { useState } from 'react';
import { Text } from '@mantine/core';
import type { Reference } from '@sceneproof/api-client';

export function ReferenceImage({ reference }: { reference: Reference }) {
  const [failed, setFailed] = useState(false);
  return failed ? <Text size="sm" c="red.3" role="alert">Reference image unavailable: {reference.title}. Historical evidence has not been replaced.</Text>
    : <img className="reference-image" src={reference.url} alt={`Reference: ${reference.title}`} onError={() => setFailed(true)} />;
}
