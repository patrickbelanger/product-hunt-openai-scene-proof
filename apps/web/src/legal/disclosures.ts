export const repositoryUrl = 'https://github.com/patrickbelanger/product-hunt-openai-scene-proof';
export const disclosureDate = '2026-09-17';

const disclosureKeys = ['VITE_LEGAL_OPERATOR', 'VITE_PRIVACY_CONTACT', 'VITE_PRIVACY_CONTACT_EMAIL', 'VITE_PRIVACY_HOSTING', 'VITE_PRIVACY_RETENTION', 'VITE_PRIVACY_TRANSFERS', 'VITE_PRIVACY_LEGAL_BASIS'] as const;
type DisclosureEnvironment = Record<string, unknown>;
const validEmail = /^[^\s@?#]+@[^\s@?#]+\.[^\s@?#]+$/;

export function validateProductionDisclosures(environment: DisclosureEnvironment) {
  const value = (key: typeof disclosureKeys[number]) => {
    const setting = environment[key];
    return typeof setting === 'string' ? setting.trim() : '';
  };
  const invalid = disclosureKeys.filter(key => /\bTODO\b/i.test(value(key)));
  if (!value('VITE_LEGAL_OPERATOR')) invalid.push('VITE_LEGAL_OPERATOR');
  if (!validEmail.test(value('VITE_PRIVACY_CONTACT_EMAIL'))) invalid.push('VITE_PRIVACY_CONTACT_EMAIL');
  if (invalid.length) throw new Error(`Legal disclosure configuration required: ${[...new Set(invalid)].join(', ')}. Supply confirmed public values before building for production.`);
}

export function disclosureSettings() {
  if (import.meta.env.PROD) validateProductionDisclosures(import.meta.env);
  const email = import.meta.env.VITE_PRIVACY_CONTACT_EMAIL?.trim() ?? '';
  return {
    operator: import.meta.env.VITE_LEGAL_OPERATOR?.trim() || 'Local development instance',
    contact: import.meta.env.VITE_PRIVACY_CONTACT?.trim() || 'Service operator',
    email: validEmail.test(email) ? email : undefined,
    hosting: import.meta.env.VITE_PRIVACY_HOSTING?.trim(),
    retention: import.meta.env.VITE_PRIVACY_RETENTION?.trim(),
    transfers: import.meta.env.VITE_PRIVACY_TRANSFERS?.trim(),
    legalBasis: import.meta.env.VITE_PRIVACY_LEGAL_BASIS?.trim(),
  };
}
