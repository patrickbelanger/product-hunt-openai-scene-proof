import { useEffect, type ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { disclosureDate, disclosureSettings, repositoryUrl } from './disclosures';

function LegalPage({ title, children }: { title: string; children: ReactNode }) {
  useEffect(() => {
    const previousTitle = document.title;
    document.title = `${title} — SceneProof`;
    return () => { document.title = previousTitle; };
  }, [title]);
  return <main id="main" className="page narrow legal-page">
    <Link to="/">← Back to projects</Link>
    <h1>{title}</h1>
    <p className="legal-date">Last updated: <time dateTime={disclosureDate}>September 17, 2026</time></p>
    {children}
  </main>;
}

function PrivacyContact() {
  const settings = disclosureSettings();
  return <dl className="legal-contact">
    <dt>Service operator</dt><dd>{settings.operator}</dd>
    <dt>Privacy contact</dt><dd>{settings.contact}</dd>
    <dt>Private contact channel</dt><dd>{settings.email ? <a href={`mailto:${encodeURIComponent(settings.email)}`}>{settings.email}</a> : 'TODO: publish a monitored privacy email address. A working private contact channel has not yet been supplied.'}</dd>
  </dl>;
}

export function PrivacyPolicy() {
  const settings = disclosureSettings();
  return <LegalPage title="Privacy Policy">
    <p>SceneProof is an early-stage film review tool. It helps creators inspect media, discover film context and review possible continuity issues. This notice describes the application’s data handling; it is not a certification or assurance of legal compliance.</p>
    <section aria-labelledby="privacy-contact"><h2 id="privacy-contact">Who operates the service</h2>
      <PrivacyContact />
      <p>Any TODO below is an unresolved operator disclosure, not a promise about the service.</p>
    </section>
    <section aria-labelledby="privacy-information"><h2 id="privacy-information">Information received and created</h2>
      <p>Creating a project sends its name, optional description and continuity rules to SceneProof. Uploads send the selected video or image and its filename. References include images, titles and creator guidance. Creator actions can include explanations, narrative scope, edits and decisions about findings or proposed anchors. Content may contain people’s images, voices or other personal information.</p>
      <p>Opening a demo sends a request identifier and creates a working copy of the bundled film, shots, references and project text. It does not upload files from your device or start AI processing. Changes and analysis results made in that copy are stored like other project content.</p>
      <p>SceneProof stores project and media identifiers, timestamps, filenames, hashes, dimensions, durations, sampled frames, analysis inputs/context snapshots, transcripts, summaries, proposed anchors, findings, evidence links, creator history, run status and error information. Provider request identifiers and usage metadata are saved when available. Project records are stored in a database; original uploads and derived media are stored separately on the server.</p>
    </section>
    <section aria-labelledby="privacy-purpose"><h2 id="privacy-purpose">Why content is processed</h2>
      <p>Processing provides project storage, media validation and frame extraction, Film Intelligence, continuity analysis, evidence inspection, creator-guided re-evaluation and saved history. Request identifiers prevent duplicate work. Operational diagnostics and resource/admission controls help operate the service and limit abuse.</p>
      <p>Uploading or editing references alone does not start an AI request. AI processing starts when an analysis or re-evaluation is explicitly requested. Browsing these legal pages does not start analysis.</p>
      <p>{settings.legalBasis}</p>
    </section>
    <section aria-labelledby="privacy-providers"><h2 id="privacy-providers">AI providers and processing locations</h2>
      <p>The implemented AI provider is OpenAI. For Film Intelligence, SceneProof extracts audio from the source film and sends it for transcription when audio is present. Sampled images, timed transcript text and source/segment identifiers are then sent for film understanding. Continuity analysis sends selected frames, reference images and guidance, project text/rules and available film memory. Targeted re-evaluation also sends the original finding, relevant evidence and your explanation/scope.</p>
      <p>These requests use OpenAI’s external API. Processing may take place outside Québec or Canada; this application does not establish Québec/Canada-only processing. Exact account settings and locations remain to be confirmed by the operator.</p>
      <p>The image/text analysis requests ask OpenAI not to store the response as application state. That setting is not a guarantee of zero provider retention, and deleting a SceneProof project does not send a deletion request to OpenAI. See <a href="https://developers.openai.com/api/docs/guides/your-data">OpenAI’s API data controls</a> for provider policies and available account controls.</p>
      <p>{settings.transfers}</p>
      <p>{settings.hosting}</p>
    </section>
    <section aria-labelledby="privacy-retention"><h2 id="privacy-retention">Retention, deletion and demo reset</h2>
      <p>There is no automatic expiry schedule for project content in the application. Saved content and history remain until deleted through supported operations or handled by the operator.</p>
      <ul>
        <li><strong>Ordinary projects:</strong> Delete project requires the exact project name and is refused while protected work is active. It removes the project’s database records, then attempts to remove its server media. If media cleanup fails, the UI reports pending cleanup and deletion can be retried. A minimal deletion record with the project identifier and cleanup timestamps remains.</li>
        <li><strong>Demo copies:</strong> Reset creates a replacement copy. It retires the old project from the library but retains the old content, media and analysis history, which remain accessible through their identifiers. Reset is not deletion. The application does not offer ordinary deletion for demo copies or automatic cleanup of retired copies; contact the operator for a privacy/deletion request.</li>
        <li><strong>References and history:</strong> Archiving a reference does not erase its image or historical evidence. Editing a reference, resolving/dismissing a finding or re-evaluating it does not erase the original history.</li>
        <li><strong>Operational records:</strong> Run identifiers and reservation timestamps for shared usage limits survive project deletion/reset. Reservations older than 24 hours are pruned during a later admitted run, not by a guaranteed expiry job. Cleanup failures or interruptions can leave residual files. Backup and log retention are not established by the application code.</li>
      </ul>
      <p>{settings.retention}</p>
    </section>
    <section aria-labelledby="privacy-browser"><h2 id="privacy-browser">Browser storage, cookies and requests</h2>
      <p>The application includes no analytics, advertising trackers or tracking scripts and does not set application cookies. Functional local storage remembers whether you completed or skipped the tour. The UI framework reads a color-scheme preference if present, while SceneProof fixes the display to dark mode. Session storage remembers demo request identity, unresolved Film Intelligence request identity/consent, and unresolved finding actions, including entered explanation and scope. These support recovery, not advertising.</p>
      <p>You can clear site storage using your browser settings. This can remove recovery information or tour preferences; it does not delete server projects or demo history. Session restoration behavior depends on your browser. The interface also holds temporary drafts and inspection selections in memory.</p>
      <p>Network requests necessarily reach the serving infrastructure with connection/request information such as an IP address and browser headers. No intentional storage of visitor IP addresses or user-agent strings was found in the application’s database or custom logs. Application diagnostics include run/asset identifiers, error codes and sanitized exception information. Hosting, proxy and infrastructure logs may differ; their settings and retention need operator confirmation.</p>
    </section>
    <section aria-labelledby="privacy-security"><h2 id="privacy-security">Access and security limits</h2>
      <p>SceneProof validates uploads, bounds resource use, keeps provider credentials server-side and applies request and storage safeguards. These measures do not guarantee security. The application has no private user accounts or per-user project access isolation: clients able to reach the API can access or modify projects, and the library is shared. Do not treat a project URL or demo identifier as a privacy control. Do not submit confidential or unauthorized personal content.</p>
    </section>
    <section aria-labelledby="privacy-rights"><h2 id="privacy-rights">Questions and privacy rights</h2>
      <p>Depending on the law that applies, including Québec privacy law or the GDPR, you may be entitled to request access, correction, deletion, a copy/portability, restriction of processing, objection, or withdrawal of consent where processing relies on consent. These rights can have conditions and exceptions. Choosing an AI action is not a substitute for the operator establishing the applicable legal grounds.</p>
      <p>Use the private contact channel above for a request or complaint. Include the project/run identifier if available and enough context to identify the information, without sending unnecessary sensitive media. Do not post personal data in public GitHub issues. The operator may need to verify your relationship to the data before acting. The current UI does not provide a full privacy-request or data-export workflow.</p>
      <p>You may also contact the <a href="https://www.cai.gouv.qc.ca/">Commission d’accès à l’information du Québec</a> or the relevant data protection authority. The <a href="https://commission.europa.eu/law/law-topic/data-protection/information-individuals_en">European Commission’s rights information</a> explains GDPR rights. AI findings assist creative review; they are not intended as decisions about people’s legal rights or eligibility.</p>
    </section>
    <p>Changes to this notice will be reflected in its last-updated date. See also the <Link to="/terms">Terms of Use</Link>.</p>
  </LegalPage>;
}

export function TermsOfUse() {
  return <LegalPage title="Terms of Use">
    <p>SceneProof is a free, early-stage film review MVP with publicly available source code. These terms describe use of the hosted service. Please also read the <Link to="/privacy">Privacy Policy</Link> before submitting content.</p>
    <section><h2>Operator and contact</h2><PrivacyContact /></section>
    <section><h2>Your content and permissions</h2>
      <p>Only submit media and text you have the rights and permission to process using SceneProof and its AI providers. This includes permission from people whose personal information appears in the content where required. Do not upload illegal material, confidential content or third-party personal information unless you are authorized to disclose and process it in this service. The shared workspace is not a private repository.</p>
      <p>You remain responsible for submitted content. By submitting it and requesting features, you authorize the processing needed to provide those features as described in the Privacy Policy. These terms do not transfer ownership of your content to SceneProof or grant other visitors a license to reuse it.</p>
    </section>
    <section><h2>AI assistance and creator responsibility</h2>
      <p>AI outputs are probabilistic: they may be incomplete, inaccurate or misleading. Sampling may miss events, and transcripts and interpretations may be wrong. Findings are assistance, not authoritative determinations. A difference does not automatically mean a continuity error. Review the evidence and context yourself; you remain responsible for creative decisions, rights clearance and final content.</p>
    </section>
    <section><h2>Acceptable use</h2>
      <p>Do not bypass usage or security limits, interfere with the service, access or modify others’ projects without permission, submit malicious files or deliberately consume resources to disrupt availability. Do not use SceneProof to make consequential decisions about people based only on AI output.</p>
    </section>
    <section><h2>Availability, warranties and responsibility</h2>
      <p>The MVP may change, be interrupted, limited or discontinued. Keep your own copies of important content. No particular availability, result, accuracy or retention period is promised.</p>
      <p>To the extent permitted by applicable law, the service is provided “as is” and “as available”, without warranties. To that same extent, the operator disclaims liability for indirect or consequential loss arising from use of, or inability to use, the service. Nothing in these terms excludes liability or rights that cannot lawfully be excluded, including mandatory consumer protections. These terms do not promise that any particular limitation is enforceable in your situation.</p>
    </section>
    <section><h2>Source code and licensing</h2>
      <p>The source is available in the <a href={repositoryUrl}>SceneProof GitHub repository</a>. Source-code permissions are governed by its <a href={`${repositoryUrl}/blob/main/LICENSE`}>LICENSE file</a>, separately from these service terms. The inspected file contains Apache License language but starts part-way through the text; the operator must clarify the complete license before relying on an open-source licensing claim. These terms do not supply missing license terms or grant rights to uploaded media.</p>
    </section>
    <p>Updates will be shown with a revised last-updated date. This disclosure draft makes no claim of legal approval or statutory compliance.</p>
  </LegalPage>;
}
