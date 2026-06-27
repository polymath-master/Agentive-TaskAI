# Custom Agent Instructions

As a senior software engineer, always adhere to the following rules for managing, updating, and extending this project:

## Project Structure & Clean Code
- **Feature Separation**: Keep all features in separate, well-organized files for future maintenance.
- **Debugging Readiness**: Structure the codebase so that it is easy to debug, trace logs, and fix bugs.
- **Commenting**: Always include clear, meaningful comments on codeblocks for human understanding and future maintainability.
- **Micro-scope Method**: Target only specific, needed files when updating or fixing bugs. Do not touch unrelated files or rewrite entire classes unnecessarily.
- **Context Preservation**: Always stay strictly in context and preserve the existing logic.
- **Verification First**: Do not write code without verifying sources or being 100% confident about the code APIs and syntax.

## Smarter Task & Feature Creation
- **Understand Before Implementing**: Never jump directly into creating a task or coding a feature. 
- **Ask Clarifying Questions**: Always ask context-relevant, task-related questions to clarify triggers, inputs, actions, and expectations. This ensures that the generated task script, automation flow, or code is highly accurate, stable, and meets exact user requirements.
- **UI/UX Excellence**: For any UI or visual-related tasks, research user-friendly design patterns, accessibility, and high-quality Material 3 layout conventions before building.

## Token Efficiency & Memory Optimization
- **Token Saving**: Save tokens actively. Avoid scanning, searching, or reading files unless explicitly needed for the current task. Focus directly on the relevant, specific sections or files.
- **Context Retention**: Make sure the coding agent remains fully aware of prior conversations, existing database configurations, and established files to prevent duplicate code or logic errors.

## Performance & Battery Resource Preservation (Critical)
- **Manual & Scheduled Work Split**: Background processes and scheduled tasks (like news brief syndication) must never run in continuous looping states or aggressive retries.
- **Targeted Executions Only**: Tasks must execute on precise, specific, user-set daily schedules or manual triggers only.
- **Remove Unnecessary Background Loops**: Background workers (such as WorkManager) must not retry endlessly on failure (always return failure rather than aggressive retry) to prevent battery drainage issues.

