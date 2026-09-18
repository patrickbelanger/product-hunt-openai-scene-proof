export const repositoryUrl = 'https://github.com/patrickbelanger/product-hunt-openai-scene-proof';
export const disclosureDate = '2026-09-17';

export function disclosureSettings() {
  const email = import.meta.env.VITE_PRIVACY_CONTACT_EMAIL?.trim() ?? '';
  return {
    operator: import.meta.env.VITE_LEGAL_OPERATOR?.trim() || 'TODO: publish the service operator’s identity and contact details.',
    contact: import.meta.env.VITE_PRIVACY_CONTACT?.trim() || 'TODO: publish the privacy contact’s name or role, title and contact details.',
    email: /^[^\s@?#]+@[^\s@?#]+\.[^\s@?#]+$/.test(email) ? email : undefined,
    hosting: import.meta.env.VITE_PRIVACY_HOSTING?.trim() || 'TODO: identify the hosting, database, storage and infrastructure providers, their locations and any infrastructure request logging.',
    retention: import.meta.env.VITE_PRIVACY_RETENTION?.trim() || 'TODO: publish the operator’s retention and deletion arrangements for retained demo copies, diagnostic logs, backups and residual files.',
    transfers: import.meta.env.VITE_PRIVACY_TRANSFERS?.trim() || 'TODO: confirm the provider account’s processing locations, retention settings and applicable international-transfer arrangements.',
    legalBasis: import.meta.env.VITE_PRIVACY_LEGAL_BASIS?.trim() || 'TODO: confirm the applicable legal grounds for each processing purpose and any relevant representative contact details.',
  };
}
