import { useCallback, useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import {
  fetchCase, fetchCaseActivity, fetchCaseDocuments, addDocument, deleteDocument, recordActivity,
  updateCaseStatus, assignCase, shareCase, fetchCaseShares, revokeCaseShare, fetchDistrictOfficers, downloadDocumentFile,
} from '../api/casesApi'
import { sendNewMessage } from '../api/messagesApi'
import { usePolling } from '../hooks/usePolling'
import AppShell from '../components/AppShell'
import StatusPill from '../components/StatusPill'
import ActivityTimeline from '../components/ActivityTimeline'
import RecipientPicker from '../components/RecipientPicker'
import MarkdownContent from '../components/MarkdownContent'
import MarkdownEditor from '../components/MarkdownEditor'
import './CaseDetailPage.css'

const STATUSES = ['OPEN', 'IN_PROGRESS', 'CLOSED']
const DISTRICTS = ['NORTH', 'EAST', 'SOUTH', 'WEST']

const DELETE_ACTION_BY_TYPE = { MESSAGE: 'COMMENT_DELETED', REPORT: 'REPORT_DELETED', FILE: 'FILE_DELETED' }
const DOCUMENT_NOUN_BY_TYPE = { MESSAGE: 'comment', REPORT: 'report', FILE: 'file' }

export default function CaseDetailPage() {
  const { id } = useParams()
  const { user, token, logout } = useAuth()
  const canManage = user.role === 'SUPERVISOR' || user.role === 'ADMIN'

  const [caseDetail, setCaseDetail] = useState(null)
  const [loadError, setLoadError] = useState(null)
  const [documents, setDocuments] = useState(null)
  const [activity, setActivity] = useState(null)
  const [shares, setShares] = useState(null)
  const [officers, setOfficers] = useState([])

  const [documentType, setDocumentType] = useState('MESSAGE')
  const [documentTitle, setDocumentTitle] = useState('')
  const [documentContent, setDocumentContent] = useState('')
  const [documentFile, setDocumentFile] = useState(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [formError, setFormError] = useState(null)

  const [statusChoice, setStatusChoice] = useState('OPEN')
  const [assigneeChoice, setAssigneeChoice] = useState('')
  const [shareChoice, setShareChoice] = useState(DISTRICTS[0])
  const [shareUsername, setShareUsername] = useState('')
  const [manageError, setManageError] = useState(null)

  const [sendRecipient, setSendRecipient] = useState('')
  const [sendNote, setSendNote] = useState('')
  const [sendError, setSendError] = useState(null)
  const [sendSuccess, setSendSuccess] = useState(null)

  const loadDocuments = useCallback(() => {
    fetchCaseDocuments(token, id).then(setDocuments)
  }, [token, id])

  const loadCase = useCallback(() => {
    fetchCase(token, id)
      .then((c) => {
        setCaseDetail(c)
        setStatusChoice(c.status)
      })
      .catch((error) => setLoadError(error.message))
  }, [token, id])

  const loadShares = useCallback(() => {
    fetchCaseShares(token, id).then(setShares)
  }, [token, id])

  const loadActivity = useCallback(() => {
    fetchCaseActivity(token, id).then(setActivity)
  }, [token, id])

  useEffect(() => {
    loadCase()
    loadDocuments()
    loadActivity()
    loadShares()
  }, [token, id, loadCase, loadDocuments, loadActivity, loadShares])

  usePolling(loadActivity, 8000)

  useEffect(() => {
    if (canManage) {
      fetchDistrictOfficers(token).then(setOfficers)
    }
  }, [canManage, token])

  async function handleAddDocument(event) {
    event.preventDefault()
    setFormError(null)

    if (documentType === 'FILE' && !documentFile) {
      setFormError('Choose a file to upload.')
      return
    }

    const isTextType = documentType === 'MESSAGE' || documentType === 'REPORT'
    if (isTextType && !documentContent.trim()) {
      setFormError('Write some content first.')
      return
    }

    setIsSubmitting(true)

    try {
      await addDocument(token, id, {
        type: documentType,
        title: documentTitle || undefined,
        content: isTextType ? documentContent : undefined,
        file: documentType === 'FILE' ? documentFile : undefined,
      })
      setDocumentTitle('')
      setDocumentContent('')
      setDocumentFile(null)
      loadDocuments()
    } catch (error) {
      setFormError(error.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  async function handleUpdateStatus() {
    setManageError(null)
    try {
      await updateCaseStatus(token, id, statusChoice)
      loadCase()
    } catch (error) {
      setManageError(error.message)
    }
  }

  async function handleAssign() {
    setManageError(null)
    try {
      await assignCase(token, id, assigneeChoice)
      loadCase()
    } catch (error) {
      setManageError(error.message)
    }
  }

  async function handleShareWithDistrict() {
    setManageError(null)
    try {
      await shareCase(token, id, { district: shareChoice })
      loadShares()
    } catch (error) {
      setManageError(error.message)
    }
  }

  async function handleShareWithUser() {
    setManageError(null)
    try {
      await shareCase(token, id, { username: shareUsername })
      setShareUsername('')
      loadShares()
    } catch (error) {
      setManageError(error.message)
    }
  }

  async function handleRevokeShare(shareId) {
    setManageError(null)
    try {
      await revokeCaseShare(token, id, shareId)
      loadShares()
    } catch (error) {
      setManageError(error.message)
    }
  }

  async function handleSendCase(event) {
    event.preventDefault()
    setSendError(null)
    setSendSuccess(null)

    try {
      await sendNewMessage(token, {
        recipientUsername: sendRecipient,
        body: sendNote || `Sharing case ${caseDetail.caseNumber}: ${caseDetail.title}`,
        relatedCaseId: caseDetail.id,
      })
      setSendRecipient('')
      setSendNote('')
      setSendSuccess('Case sent.')
    } catch (error) {
      setSendError(error.message)
    }
  }

  async function handleDownloadFile(doc) {
    setFormError(null)
    try {
      const blob = await downloadDocumentFile(token, id, doc.id)
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = doc.originalFilename
      link.click()
      URL.revokeObjectURL(url)
    } catch (error) {
      setFormError(error.message)
    }
  }

  function canDeleteDocument(doc) {
    return canManage || doc.authorUsername === user.username
  }

  async function handleDeleteDocument(doc) {
    setFormError(null)
    try {
      await deleteDocument(token, id, doc.id)
      await recordActivity(token, {
        entityType: 'CASE',
        entityId: id,
        action: DELETE_ACTION_BY_TYPE[doc.type],
        details: `Deleted a ${DOCUMENT_NOUN_BY_TYPE[doc.type]}` + (doc.title ? `: ${doc.title}` : ''),
      })
      loadDocuments()
      loadActivity()
    } catch (error) {
      setFormError(error.message)
    }
  }

  if (loadError) {
    return (
      <AppShell user={user} onSignOut={logout}>
        <p className="case-detail__loading" role="alert">
          Case not found or you don't have access to it.
        </p>
      </AppShell>
    )
  }

  if (!caseDetail) {
    return (
      <AppShell user={user} onSignOut={logout}>
        <p className="case-detail__loading">Loading case…</p>
      </AppShell>
    )
  }

  return (
    <AppShell user={user} onSignOut={logout}>
      <div className="case-detail">
        <header className="case-detail__header">
          <span className="case-detail__number">{caseDetail.caseNumber}</span>
          <h1>{caseDetail.title}</h1>
          <div className="case-detail__pills">
            <StatusPill value={caseDetail.status} />
            <StatusPill value={caseDetail.district} />
          </div>
          <p>{caseDetail.description}</p>
          {caseDetail.assignedOfficerUsername && (
            <p className="case-detail__assignee">Assigned to {caseDetail.assignedOfficerUsername}</p>
          )}
        </header>

        <section className="card case-detail__section">
          <h2>Manage</h2>
          {manageError && <p className="auth-form__error" role="alert">{manageError}</p>}

          <div className="case-manage-row">
            <label htmlFor="statusChoice">Status</label>
            <select id="statusChoice" value={statusChoice} onChange={(e) => setStatusChoice(e.target.value)}>
              {STATUSES.map((s) => (
                <option key={s} value={s}>{s.replace('_', ' ')}</option>
              ))}
            </select>
            <button type="button" className="btn btn-ghost-dark" onClick={handleUpdateStatus}>Update status</button>
          </div>

          {canManage && (
            <>
              <div className="case-manage-row">
                <label htmlFor="assigneeChoice">Assign to</label>
                <select id="assigneeChoice" value={assigneeChoice} onChange={(e) => setAssigneeChoice(e.target.value)}>
                  <option value="" disabled>Select an officer…</option>
                  {officers.map((o) => (
                    <option key={o.username} value={o.username}>{o.fullName} ({o.username})</option>
                  ))}
                </select>
                <button type="button" className="btn btn-ghost-dark" onClick={handleAssign} disabled={!assigneeChoice}>Assign</button>
              </div>

              <div className="case-manage-row">
                <label htmlFor="shareChoice">Share with district</label>
                <select id="shareChoice" value={shareChoice} onChange={(e) => setShareChoice(e.target.value)}>
                  {DISTRICTS.filter((d) => d !== caseDetail.district).map((d) => (
                    <option key={d} value={d}>{d}</option>
                  ))}
                </select>
                <button type="button" className="btn btn-ghost-dark" onClick={handleShareWithDistrict}>Share district</button>
              </div>

              <div className="case-manage-row">
                <RecipientPicker
                  token={token}
                  id="shareRecipientSearch"
                  label="Share with user"
                  selectedUsername={shareUsername}
                  onSelect={(selected) => setShareUsername(selected.username)}
                />
                <button type="button" className="btn btn-ghost-dark" onClick={handleShareWithUser} disabled={!shareUsername}>Share user</button>
              </div>
            </>
          )}

          {shares !== null && shares.length > 0 && (
            <ul className="case-shares-list">
              {shares.map((s) => (
                <li key={s.id}>
                  Shared with {s.sharedWithDistrict ?? s.sharedWithUsername} by {s.sharedByUsername}
                  {canManage && (
                    <button type="button" className="btn btn-ghost-dark" onClick={() => handleRevokeShare(s.id)}>
                      Remove
                    </button>
                  )}
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className="card case-detail__section">
          <h2>Send this case</h2>
          {sendError && <p className="auth-form__error" role="alert">{sendError}</p>}
          {sendSuccess && <p>{sendSuccess}</p>}

          <form onSubmit={handleSendCase} className="case-manage-row">
            <RecipientPicker
              token={token}
              selectedUsername={sendRecipient}
              onSelect={(selected) => setSendRecipient(selected.username)}
            />

            <label htmlFor="sendNote">Note (optional)</label>
            <input id="sendNote" value={sendNote} onChange={(e) => setSendNote(e.target.value)} />

            <button type="submit" className="btn btn-ghost-dark" disabled={!sendRecipient}>Send case</button>
          </form>
        </section>

        <section className="card case-detail__section">
          <h2>Documents</h2>
          {documents === null && <p>Loading…</p>}
          {documents !== null && documents.length === 0 && <p>No documents yet.</p>}
          {documents !== null && documents.map((doc) => (
            <article key={doc.id} className="case-document">
              <div className="case-document__meta">
                <StatusPill value={doc.type === 'MESSAGE' ? 'COMMENT' : doc.type} />
                <span>{doc.authorFullName}</span>
                <span>{new Date(doc.createdAt).toLocaleString()}</span>
              </div>
              {doc.title && <h3>{doc.title}</h3>}
              <MarkdownContent content={doc.content} />
              {doc.type === 'FILE' && (
                <button type="button" className="case-document__download" onClick={() => handleDownloadFile(doc)}>
                  {doc.originalFilename}
                </button>
              )}
              {canDeleteDocument(doc) && (
                <button type="button" className="btn btn-ghost-dark" onClick={() => handleDeleteDocument(doc)}>
                  Delete
                </button>
              )}
            </article>
          ))}

          <form onSubmit={handleAddDocument} className="case-document-form">
            {formError && <p className="auth-form__error" role="alert">{formError}</p>}

            <label htmlFor="documentType">Document type</label>
            <select id="documentType" value={documentType} onChange={(e) => setDocumentType(e.target.value)}>
              <option value="MESSAGE">Comment</option>
              <option value="REPORT">Report</option>
              <option value="FILE">File</option>
            </select>

            {(documentType === 'REPORT' || documentType === 'FILE') && (
              <>
                <label htmlFor="documentTitle">Title</label>
                <input
                  id="documentTitle"
                  value={documentTitle}
                  onChange={(e) => setDocumentTitle(e.target.value)}
                />
              </>
            )}

            {(documentType === 'MESSAGE' || documentType === 'REPORT') && (
              <MarkdownEditor
                id="documentContent"
                label="Content"
                value={documentContent}
                onChange={setDocumentContent}
                rows={documentType === 'REPORT' ? 10 : 4}
                required
              />
            )}

            {documentType === 'FILE' && (
              <>
                <label htmlFor="documentFile">File</label>
                <input
                  id="documentFile"
                  type="file"
                  accept=".jpg,.jpeg,.png,.pdf,.txt"
                  onChange={(e) => setDocumentFile(e.target.files[0] ?? null)}
                />
              </>
            )}

            <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
              Add {documentType === 'MESSAGE' ? 'comment' : documentType === 'REPORT' ? 'report' : 'file'}
            </button>
          </form>
        </section>

        <section className="card case-detail__section">
          <h2>Activity</h2>
          <ActivityTimeline entries={activity} />
        </section>
      </div>
    </AppShell>
  )
}
