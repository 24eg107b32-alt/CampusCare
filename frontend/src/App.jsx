import { useEffect, useState } from 'react'
import { ArrowDown, ArrowRight, Building2, Check, CheckCircle2, ClipboardList, HeartHandshake, House, RefreshCw, Send, ShieldCheck, Sparkles, Waves } from 'lucide-react'
import { createComplaint, getAdmin, getComplaints, loginAdmin, loginStudent, logoutAdmin, registerStudent, updateComplaintStatus } from './api.js'

const STATUSES = ['Pending', 'In Progress', 'Resolved']
const EMPTY_FORM = { category: '', location: '', title: '', description: '' }

function App() {
  const [complaints, setComplaints] = useState([])
  const [currentUser, setCurrentUser] = useState(null)
  const [authChecking, setAuthChecking] = useState(true)
  const [loginForm, setLoginForm] = useState({ username: '', password: '' })
  const [studentCredentials, setStudentCredentials] = useState({ email: '', password: '' })
  const [studentAuthMode, setStudentAuthMode] = useState('login')
  const [authError, setAuthError] = useState('')
  const [authBusy, setAuthBusy] = useState(false)
  const [form, setForm] = useState(EMPTY_FORM)
  const [notice, setNotice] = useState(null)
  const [loadError, setLoadError] = useState('')
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [updatingId, setUpdatingId] = useState(null)

  async function refreshComplaints() {
    setLoading(true)
    try {
      setComplaints(await getComplaints())
      setLoadError('')
    } catch (error) {
      setLoadError(error.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    let active = true
    getAdmin()
      .then((currentAdmin) => {
        if (!active) return
        setCurrentUser(currentAdmin)
        if (currentAdmin.role === 'ADMIN') {
          return getComplaints().then((items) => {
            if (active) setComplaints(items)
          })
        }
      })
      .catch((error) => {
        if (active && error.status !== 401) setLoadError(error.message)
      })
      .finally(() => {
        if (active) {
          setLoading(false)
          setAuthChecking(false)
        }
      })
    return () => { active = false }
  }, [])

  const totals = {
    total: complaints.length,
    pending: complaints.filter((item) => item.status === 'Pending').length,
    progress: complaints.filter((item) => item.status === 'In Progress').length,
    resolved: complaints.filter((item) => item.status === 'Resolved').length,
  }

  function updateField(event) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }))
  }

  async function submitLogin(event) {
    event.preventDefault()
    setAuthBusy(true)
    setAuthError('')
    try {
      const signedInAdmin = await loginAdmin(loginForm)
      setCurrentUser(signedInAdmin)
      setLoginForm({ username: '', password: '' })
      await refreshComplaints()
    } catch (error) {
      setAuthError(error.message)
    } finally {
      setAuthBusy(false)
    }
  }

  async function submitStudentAuth(event) {
    event.preventDefault()
    setAuthBusy(true)
    setAuthError('')
    try {
      const signedInUser = studentAuthMode === 'register'
        ? await registerStudent(studentCredentials)
        : await loginStudent(studentCredentials)
      setCurrentUser(signedInUser)
      setStudentCredentials({ email: '', password: '' })
      setNotice({ type: 'success', text: studentAuthMode === 'register' ? 'Account created. You can submit a complaint now.' : 'You are signed in. You can submit a complaint now.' })
    } catch (error) {
      setAuthError(error.message)
    } finally {
      setAuthBusy(false)
    }
  }

  async function signOut() {
    try {
      await logoutAdmin()
    } finally {
      setCurrentUser(null)
      setComplaints([])
      setLoadError('')
    }
  }

  async function submitComplaint(event) {
    event.preventDefault()
    setSubmitting(true)
    setNotice(null)
    try {
      await createComplaint(form)
      if (currentUser?.role === 'ADMIN') await refreshComplaints()
      setForm(EMPTY_FORM)
      setNotice({ type: 'success', text: 'Your complaint has been submitted. We’ll keep you updated.' })
    } catch (error) {
      setNotice({ type: 'error', text: error.message })
    } finally {
      setSubmitting(false)
    }
  }

  async function changeStatus(id, status) {
    const previous = complaints
    setUpdatingId(id)
    setComplaints((current) => current.map((item) => item.id === id ? { ...item, status } : item))
    try {
      const updated = await updateComplaintStatus(id, status)
      setComplaints((current) => current.map((item) => item.id === id ? updated : item))
    } catch (error) {
      setComplaints(previous)
      setLoadError(error.message)
    } finally {
      setUpdatingId(null)
    }
  }

  return (
    <>
      <header className="site-header">
        <a className="brand" href="#home" aria-label="CampusCare home">
          <span className="brand-mark"><HeartHandshake size={22} strokeWidth={2.2} /></span>
          <span>campus<span className="brand-light">care</span><small>YOUR CAMPUS, HEARD</small></span>
        </a>
        <nav className="main-nav" aria-label="Main navigation">
          <a href="#home">Home</a>
          <a href="#services">Services</a>
          <a href="#submit">Submit complaint</a>
          <a href="#help">Get help</a>
          <a href="#submit">{currentUser?.role === 'STUDENT' ? currentUser.email : 'Student login / register'}</a>
          <a href="#dashboard">{currentUser?.role === 'ADMIN' ? 'Admin dashboard' : 'Admin login'}</a>
        </nav>
        <a className="nav-cta" href="#submit">Raise a concern <ArrowRight size={15} /></a>
      </header>

      <main>
        <section className="hero" id="home">
          <div className="hero-copy">
            <p className="eyebrow"><span className="eyebrow-dot" /> A better campus starts with a conversation</p>
            <h1>Your campus.<br /><span>Your voice.</span></h1>
            <p className="hero-description">A leaky tap, a broken light, something that just doesn’t feel right. Tell us what’s going on and we’ll help get it sorted.</p>
            <div className="hero-actions">
              <a className="button button-primary" href="#submit">Submit a complaint <ArrowRight size={17} /></a>
            </div>
            <div className="hero-proof"><span className="proof-icons"><i><Check size={13} /></i><i><Check size={13} /></i><i><Check size={13} /></i></span><span>A campus that listens, together.</span></div>
          </div>
          <div className="hero-art" aria-label="A welcoming campus courtyard illustration" role="img">
            <div className="art-sun" />
            <div className="art-cloud cloud-one" /><div className="art-cloud cloud-two" />
            <div className="art-hill hill-back" /><div className="art-hill hill-front" />
            <div className="campus-building"><div className="building-roof" /><div className="building-body"><div className="building-window-row"><i /><i /><i /></div><div className="building-door" /><div className="building-window-row lower"><i /><i /><i /></div></div></div>
            <div className="art-tree tree-left"><i /><b /><span /></div><div className="art-tree tree-right"><i /><b /><span /></div>
            <div className="art-path" />
            <div className="art-note"><span><Sparkles size={17} /></span><div><strong>We’re listening.</strong><small>Every concern matters.</small></div></div>
            <span className="art-caption">A place to learn, live & belong.</span>
          </div>
          <div className="hero-bottom"><span>01 / 03</span><span className="hero-line" /><span>CARE THAT MOVES CAMPUS FORWARD</span><a href="#services" aria-label="Explore services"><ArrowDown size={17} /></a></div>
        </section>

        <section className="services section-wrap" id="services">
          <div className="section-heading"><div><p className="eyebrow">HERE FOR THE EVERYDAY</p><h2>Small issue or big concern,<br /><span>there’s a place for it.</span></h2></div><p>Campus life comes with a lot of moving parts. We’ll help you find the right people to get things moving again.</p></div>
          <div className="service-grid">
            <article className="service-card service-college"><span className="service-icon"><Building2 size={21} /></span><span className="service-number">01</span><h3>College issues</h3><p>Classrooms, campus facilities, academics, or anything affecting your day at college.</p><a href="#submit" aria-label="Report a college issue"><ArrowRight size={18} /></a></article>
            <article className="service-card service-hostel"><span className="service-icon"><House size={21} /></span><span className="service-number">02</span><h3>Hostel issues</h3><p>Maintenance, utilities, shared spaces, and the things that make a room feel like home.</p><a href="#submit" aria-label="Report a hostel issue"><ArrowRight size={18} /></a></article>
            <article className="service-card service-track"><span className="service-icon"><ClipboardList size={21} /></span><span className="service-number">03</span><h3>Admin tracking</h3><p>Administrators review submitted complaints and update each report’s status from the secure dashboard.</p><a href="#dashboard" aria-label="Admin login for complaint tracking"><ArrowRight size={18} /></a></article>
          </div>
        </section>

        <section className="submit-section" id="submit">
          <div className="submit-inner">
            <div className="submit-intro"><p className="eyebrow">LET’S GET IT SORTED</p><h2>It starts with<br /><span>speaking up.</span></h2><p>Share a few details and your concern will reach the right people. We’ll take it from there.</p><div className="privacy-note"><ShieldCheck size={18} /><span>Your report is handled with care<br />and shared only with the people who can help.</span></div></div>
            {!currentUser ? <div className="complaint-form student-auth-form">
              <div className="form-topline"><span>STUDENT ACCOUNT</span><span>EMAIL + PASSWORD</span></div>
              <div className="auth-tabs" role="tablist" aria-label="Student account access">
                <button type="button" role="tab" aria-selected={studentAuthMode === 'login'} className={studentAuthMode === 'login' ? 'active' : ''} onClick={() => { setStudentAuthMode('login'); setAuthError('') }}>Log in</button>
                <button type="button" role="tab" aria-selected={studentAuthMode === 'register'} className={studentAuthMode === 'register' ? 'active' : ''} onClick={() => { setStudentAuthMode('register'); setAuthError('') }}>Register</button>
              </div>
              <form onSubmit={submitStudentAuth}>
                <label>Email<input name="email" type="email" value={studentCredentials.email} onChange={(event) => setStudentCredentials((current) => ({ ...current, email: event.target.value }))} autoComplete="email" required /></label>
                <label>Password<input name="password" type="password" value={studentCredentials.password} onChange={(event) => setStudentCredentials((current) => ({ ...current, password: event.target.value }))} autoComplete={studentAuthMode === 'register' ? 'new-password' : 'current-password'} required /></label>
                {authError && <p className="form-notice error" role="alert">{authError}</p>}
                <div className="form-actions"><button className="button button-primary" type="submit" disabled={authBusy}>{authBusy ? 'Please wait…' : studentAuthMode === 'register' ? 'Create account' : 'Student login'} <ArrowRight size={15} /></button></div>
              </form>
            </div> : <form className="complaint-form" onSubmit={submitComplaint}>
              <div className="signed-in-student"><span><ShieldCheck size={14} /> Signed in as {currentUser.email}</span><button className="clear-button" type="button" onClick={signOut}>Sign out</button></div>
              <div className="form-topline"><span>NEW COMPLAINT</span><span>ALL FIELDS REQUIRED <i>*</i></span></div>
              <label>What’s it about?<select name="category" value={form.category} onChange={updateField} required><option value="" disabled>Select a category</option><option value="College">College issue</option><option value="Hostel">Hostel issue</option></select></label>
              <label>Where is it happening?<input name="location" value={form.location} onChange={updateField} placeholder="e.g. North Block, Room 204" maxLength="160" required /></label>
              <label>Give it a short title<input name="title" value={form.title} onChange={updateField} placeholder="e.g. Water leak in the hallway" maxLength="160" required /></label>
              <label>Tell us a little more<textarea name="description" value={form.description} onChange={updateField} placeholder="What happened? When did you notice it?" rows="4" maxLength="2000" required /></label>
              {notice && <p className={`form-notice ${notice.type}`} role="status">{notice.type === 'success' ? <CheckCircle2 size={17} /> : <Waves size={17} />}{notice.text}</p>}
              <div className="form-actions"><button className="button button-primary" type="submit" disabled={submitting}>{submitting ? 'Sending…' : 'Send complaint'} <Send size={15} /></button><button className="clear-button" type="reset" onClick={() => { setForm(EMPTY_FORM); setNotice(null) }}>Clear form</button></div>
            </form>}
          </div>
        </section>

        <section className="dashboard section-wrap" id="dashboard">
          {currentUser?.role !== 'ADMIN' ? <div className="admin-access">
            <div className="admin-access-heading"><p className="eyebrow">RESTRICTED AREA</p><h2>Admin <span>login.</span></h2><p>Sign in to review reports and update complaint progress.</p></div>
            {authChecking ? <p className="auth-loading" role="status">Checking admin session…</p> : <form className="complaint-form admin-login-form" onSubmit={submitLogin}>
              <div className="form-topline"><span>ADMIN SIGN IN</span><span>AUTHORIZED STAFF ONLY</span></div>
              <label>Username<input name="username" value={loginForm.username} onChange={(event) => setLoginForm((current) => ({ ...current, username: event.target.value }))} autoComplete="username" required /></label>
              <label>Password<input name="password" type="password" value={loginForm.password} onChange={(event) => setLoginForm((current) => ({ ...current, password: event.target.value }))} autoComplete="current-password" required /></label>
              {authError && <p className="form-notice error" role="alert">{authError}</p>}
              <div className="form-actions"><button className="button button-primary" type="submit" disabled={authBusy}>{authBusy ? 'Signing in…' : 'Admin login'} <ArrowRight size={15} /></button></div>
            </form>}
          </div> : <>
          <div className="dashboard-heading"><div><p className="eyebrow">CAMPUSCARE OVERVIEW</p><h2>Concerns, <span>in progress.</span></h2></div><button className="refresh-button" type="button" onClick={refreshComplaints} disabled={loading}><RefreshCw size={15} className={loading ? 'spin' : ''} />{loading ? 'Refreshing' : 'Refresh complaints'}</button></div>
          {loadError && <p className="dashboard-error" role="alert">{loadError}</p>}
          <div className="admin-session"><span><ShieldCheck size={14} /> Signed in as {currentUser.email}</span><button className="clear-button" type="button" onClick={signOut}>Sign out</button></div>
          <div className="stats-grid"><article className="stat-card stat-total"><span>ALL COMPLAINTS</span><strong>{totals.total}</strong><small><ClipboardList size={14} /> Across campus</small></article><article className="stat-card"><span>PENDING</span><strong>{totals.pending}</strong><small><i className="status-dot pending-dot" /> Awaiting review</small></article><article className="stat-card"><span>IN PROGRESS</span><strong>{totals.progress}</strong><small><i className="status-dot progress-dot" /> Being looked into</small></article><article className="stat-card"><span>RESOLVED</span><strong>{totals.resolved}</strong><small><i className="status-dot resolved-dot" /> Taken care of</small></article></div>
          <div className="table-heading"><div><h3>All complaints</h3><span>{complaints.length} {complaints.length === 1 ? 'report' : 'reports'}</span></div><span className="admin-label"><ShieldCheck size={14} /> ADMIN VIEW</span></div>
          <div className="table-scroll"><table><thead><tr><th>ID</th><th>CATEGORY</th><th>LOCATION</th><th>TITLE & DESCRIPTION</th><th>STATUS</th></tr></thead><tbody>
            {complaints.map((item) => <tr key={item.id}><td className="id-cell">#{item.id}</td><td><span className={`category-tag ${item.category?.toLowerCase()}`}>{item.category}</span></td><td className="location-cell">{item.location}</td><td className="detail-cell"><strong>{item.title}</strong><span>{item.description}</span></td><td><select className={`status-select ${item.status?.toLowerCase().replace(' ', '-')}`} value={item.status} disabled={updatingId === item.id} onChange={(event) => changeStatus(item.id, event.target.value)} aria-label={`Change status for complaint ${item.id}`}>{STATUSES.map((status) => <option key={status}>{status}</option>)}</select></td></tr>)}
            {!loading && complaints.length === 0 && <tr><td className="empty-cell" colSpan="5">No complaints yet. New reports will appear here.</td></tr>}
            {loading && complaints.length === 0 && <tr><td className="empty-cell" colSpan="5">Loading complaints…</td></tr>}
          </tbody></table></div>
          </>}
        </section>

        <section className="help-strip" id="help"><span className="help-mark"><HeartHandshake size={23} /></span><div><p className="eyebrow">NEED A HAND?</p><h2>Not sure where to start?</h2><p>Choose the closest category and tell us what you know. We’ll help route it.</p></div><a className="button button-light" href="#submit">Talk to CampusCare <ArrowRight size={16} /></a></section>
      </main>

      <footer className="site-footer"><a className="brand footer-brand" href="#home"><span className="brand-mark"><HeartHandshake size={20} /></span><span>campus<span className="brand-light">care</span></span></a><span>A little better, together. <i>© 2026 CampusCare</i></span><a href="#home">Back to top ↑</a></footer>
    </>
  )
}

export default App
