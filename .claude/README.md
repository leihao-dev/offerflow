# `.claude` Workflow Guide

This directory contains the CC/Superpowers project configuration for OfferFlow.

## What it is for

- `skills/` defines reusable project-specific coaching behavior.
- `hooks/` injects lightweight reminders and context at key moments.
- Together they make the repo easier to work on repeatedly, not just once.

## What is included

### `skills/offerflow-coach/SKILL.md`
A project coach that helps with:

- logging job applications
- logging interview debriefs
- writing honest `JOURNAL.md` entries
- reviewing weekly CC/Superpowers usage

### `hooks/hooks.json`
Project hooks that keep the workflow visible:

- `sessionStart` runs `hooks/session-start`
- `postEdit` runs `hooks/post-edit`
- `preResponse` runs `hooks/pre-response`

### `hooks/session-start`
The shell script used by `sessionStart`.

### `hooks/post-edit`
A small shell script that reminds the model to sync docs when behavior changes.

### `hooks/pre-response`
A small shell script that nudges the model to answer workflow questions concretely and repo-specifically.

## Why this exists

The goal is not to create extra ceremony. The goal is to make CC behavior consistent enough that the repo is self-explanatory:

- the app code stays focused on OfferFlow
- the journal stays honest and structured
- the assistant keeps nudging toward the same real workflow

## How to use it

1. Keep the skill and hooks in sync with the actual repo workflow.
2. When app behavior changes, update the docs and journal too.
3. Treat this folder as part of the deliverable, not as decoration.

## Maintenance rule

If a change affects how OfferFlow is used, check whether one of these should also be updated:

- `README.md`
- `JOURNAL.md`
- `.claude/skills/offerflow-coach/SKILL.md`
- `.claude/hooks/hooks.json`
- `.claude/hooks/post-edit`
- `.claude/hooks/pre-response`
