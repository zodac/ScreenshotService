CREATE TABLE IF NOT EXISTS screenshot_request (
    request_id SERIAL PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL DEFAULT NOW(),
    urls text[] NOT NULL
);
CREATE INDEX index_request_id
    ON screenshot_request(request_id);


CREATE TABLE IF NOT EXISTS screenshot_result (
    job_id int4 PRIMARY KEY,
    completed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    screenshot_filepaths text[] NOT NULL
);
CREATE INDEX index_job_id
    ON screenshot_result(job_id);