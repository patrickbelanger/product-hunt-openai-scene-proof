import { Text } from '@mantine/core';

export function UploadDisclosure() {
  return <Text size="xs" c="dimmed" className="upload-disclosure">Only upload media you have the right to process. When you request AI analysis, selected content or derived images, audio and text may be sent to OpenAI. Uploading alone does not start AI processing. <a href="/privacy" target="_blank" rel="noopener noreferrer">Privacy details (opens a new tab)</a>.</Text>;
}
