-- Hibernate 6 SchemaExport 생성 (dialect: PostgreSQL). 실제 스키마는 ddl-auto 가 관리, 이 파일은 참고본.
-- 생성일: 2026-09-04 · 소스: com.skala.fixguide.**.entity (Spring Boot 3.3.5 / Hibernate 6.5, PostgreSQL 16 Testcontainers)
-- JSON 컬럼은 PoC 에서 text + JsonMapConverter 로 저장 (목표 타입 jsonb 는 DBML/erd.md 참고).


    create table agent_results (
        edited boolean not null,
        created_at timestamp(6) with time zone not null,
        updated_at timestamp(6) with time zone not null,
        agent_code varchar(10) not null check (agent_code in ('A1','A2','A3')),
        id uuid not null,
        run_id uuid not null,
        original_json text,
        payload_json text not null,
        primary key (id),
        constraint uk_result_run_agent unique (run_id, agent_code)
    );

    create table agent_runs (
        created_at timestamp(6) with time zone not null,
        finished_at timestamp(6) with time zone,
        started_at timestamp(6) with time zone not null,
        updated_at timestamp(6) with time zone not null,
        ai_config_id uuid,
        id uuid not null,
        work_request_id uuid not null,
        status varchar(20) not null check (status in ('RUNNING','DONE','FAILED')),
        input_snapshot text,
        primary key (id)
    );

    create table agent_steps (
        finished_at timestamp(6) with time zone,
        started_at timestamp(6) with time zone,
        agent_code varchar(10) not null check (agent_code in ('A1','A2','A3')),
        id uuid not null,
        run_id uuid not null,
        status varchar(20) not null check (status in ('WAITING','RUNNING','DONE','FAILED')),
        message varchar(200),
        error_message text,
        primary key (id),
        constraint uk_step_run_agent unique (run_id, agent_code)
    );

    create table ai_configs (
        egress_allowed boolean not null,
        is_active boolean not null,
        max_tokens integer,
        temperature numeric(3,2),
        created_at timestamp(6) with time zone not null,
        updated_at timestamp(6) with time zone not null,
        agent_code varchar(10) not null check (agent_code in ('A1','A2','A3')),
        id uuid not null,
        provider varchar(20) not null,
        prompt_version varchar(30),
        model_name varchar(60),
        primary key (id)
    );

    create table approvals (
        decided_at timestamp(6) with time zone not null,
        approver_id uuid not null,
        id uuid not null,
        work_request_id uuid not null,
        decision varchar(20) not null check (decision in ('APPROVE','REJECT')),
        reason_category varchar(50),
        reason text,
        primary key (id)
    );

    create table users (
        created_at timestamp(6) with time zone not null,
        updated_at timestamp(6) with time zone not null,
        id uuid not null,
        name varchar(20) not null,
        role varchar(20) not null check (role in ('ENGINEER','SAFETY_MANAGER')),
        email varchar(255) not null unique,
        password_hash varchar(255) not null,
        primary key (id)
    );

    create table work_request_photos (
        size integer not null,
        uploaded_at timestamp(6) with time zone not null,
        id uuid not null,
        work_request_id uuid not null,
        storage_key varchar(500) not null,
        thumbnail_key varchar(500) not null,
        file_name varchar(255) not null,
        primary key (id)
    );

    create table work_requests (
        created_at timestamp(6) with time zone not null,
        submitted_at timestamp(6) with time zone,
        updated_at timestamp(6) with time zone not null,
        id uuid not null,
        requester_id uuid not null,
        request_no varchar(20) unique,
        status varchar(20) not null check (status in ('DRAFT','AI_RUNNING','AI_DONE','PENDING','APPROVED','REJECTED')),
        product_type varchar(30) check (product_type in ('VALVE','FITTING_TUBE','REGULATOR','FILTER','ETC')),
        equipment varchar(100),
        line varchar(100),
        substance varchar(100),
        product_name varchar(200),
        engineer_note text,
        operating_condition text,
        site_memo text,
        spec_json text,
        symptom text,
        primary key (id)
    );

    alter table if exists agent_results 
       add constraint FK9bqu6wx6dwfo7uk8d6ohxolfx 
       foreign key (run_id) 
       references agent_runs;

    alter table if exists agent_runs 
       add constraint FK341hn3wfs96wsw6pbq7u02p8i 
       foreign key (ai_config_id) 
       references ai_configs;

    alter table if exists agent_runs 
       add constraint FK43e4hsnqp7lssakshdiacnc63 
       foreign key (work_request_id) 
       references work_requests;

    alter table if exists agent_steps 
       add constraint FKir0qir1l8gxt6nq85nsb8srdt 
       foreign key (run_id) 
       references agent_runs;

    alter table if exists approvals 
       add constraint FKkh8kt9y5pin7d4qxs8i7pak8d 
       foreign key (approver_id) 
       references users;

    alter table if exists approvals 
       add constraint FK5kinv7j4e1xk6kn3hlbtun91t 
       foreign key (work_request_id) 
       references work_requests;

    alter table if exists work_request_photos 
       add constraint FKt3w5syyerfc96b1ns4r7jvesr 
       foreign key (work_request_id) 
       references work_requests;

    alter table if exists work_requests 
       add constraint FKjdw9w6ge6r4gv2rmo95plllif 
       foreign key (requester_id) 
       references users;
