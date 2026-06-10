CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE appointments
    ADD CONSTRAINT no_overlapping_appointments
        EXCLUDE USING gist (
            staff_id WITH =,
            tsrange(start_time, end_time, '[)') WITH &&
        )
        WHERE (status <> 'CANCELLED');
