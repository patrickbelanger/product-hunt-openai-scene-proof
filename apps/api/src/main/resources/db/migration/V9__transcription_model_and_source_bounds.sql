ALTER TABLE film_understanding_runs ALTER COLUMN transcription_model SET DEFAULT 'gpt-4o-transcribe-diarize';
ALTER TABLE film_transcript_segments ALTER COLUMN timestamp_origin SET DEFAULT 'OPENAI_DIARIZED_SEGMENT_ESTIMATE_SOURCE_START';
