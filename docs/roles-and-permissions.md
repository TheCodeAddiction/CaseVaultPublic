# CaseVault — Who Can Do What

- Every user has **one role** (`OFFICER` / `SUPERVISOR` / `ADMIN`) and **one district** (`NORTH` / `EAST` / `SOUTH` / `WEST`).
- ✅ allowed · ⛔ not allowed
- **Own district + shared** = cases in your own district, cases shared with your district, cases shared with you personally.
- Officers and supervisors reach the same cases — supervisor only adds actions.
- **No history** = the account has no assigned cases, authored documents, case shares, messages or activity-log entries.

## Before sign-in

| Action | Anyone |
|---|---|
| Register an account | ✅ always becomes an `OFFICER`, in the district chosen at registration |
| Reset a forgotten password | ✅ code sent to the account's e-mail |

## Cases

| Action | OFFICER | SUPERVISOR | ADMIN |
|---|---|---|---|
| List / search / filter / export cases | Own district + shared | Own district + shared | All districts |
| Dashboard statistics | Own district + shared | Own district + shared | All districts |
| Open a case + its activity log | Own district + shared | Own district + shared | All districts |
| Create a case | ✅ own district, auto-assigned to creator | ✅ same | ✅ same |
| Change case status (close / reopen) | Own district + shared | Own district + shared | All districts |
| Assign a case to an `OFFICER` in the case's district | ⛔ | Own district + shared | All districts |
| Share a case / revoke a share | ⛔ | Own district's cases only | ✅ any case |
| See who a case is shared with | Own district + shared | Own district + shared | All districts |
| Admin case list (every case, every district) | ⛔ | ⛔ | ✅ |
| **Delete a case** | ⛔ | ⛔ | ⛔ |

## Case documents (comments, reports, files)

| Action | OFFICER | SUPERVISOR | ADMIN |
|---|---|---|---|
| Read the document log | Own district + shared | Own district + shared | All districts |
| Add comment / report / file | Own district + shared | Own district + shared | All districts |
| Download attached file | Own district + shared | Own district + shared | All districts |
| Delete a document | Own documents only | Own documents + any on a case in own district | Any document |

## Reporting

| Action | OFFICER | SUPERVISOR | ADMIN |
|---|---|---|---|
| District report: status counts + officer workload | ⛔ | Own district only | Any district |

## Accounts

| Action | OFFICER | SUPERVISOR | ADMIN |
|---|---|---|---|
| Edit own name / e-mail / password / picture | ✅ | ✅ | ✅ |
| Change own role or district from the profile page | ⛔ | ⛔ | ⛔ |
| Delete own account | ✅ password + no history | ✅ same | ✅ same |
| See another user's picture | ✅ | ✅ | ✅ |
| List users | ⛔ | Officers in own district | Everyone |
| Create user with chosen role / district | ⛔ | ⛔ | ✅ |
| Change another user's role / district | ⛔ | ⛔ | ✅ any user, incl. themselves |
| Deactivate / reactivate account | ⛔ | ⛔ | ✅ any user, incl. themselves |
| Delete another user's account | ⛔ | ⛔ | ✅ no history only |
| See a user's account activity log | ⛔ | ⛔ | ✅ |
| Sign in while deactivated | ⛔ | ⛔ | ⛔ |

## Messaging

| Action | All roles |
|---|---|
| Search users, start conversation, send message | ✅ any other user, any district, any role (search lists active users only) |
| Read conversation, reply, mark read, download attachment | Participants only (sender + recipient) |
| Link a message to a case | Only a case the **sender** can see — recipient sees case number + title, no access to the case |
