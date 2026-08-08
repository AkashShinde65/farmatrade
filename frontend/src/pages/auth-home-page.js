import { Link } from 'react-router-dom';
import heroHandshake from '../assets/hero-handshake.png';

const FEATURES = [
  {
    title: 'Live auctions',
    text: 'Time-boxed bidding with anti-snipe extension on late bids.',
    icon: (
      <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M14 5l5 5" />
        <path d="M3 21l6-6" />
        <path d="M12.5 6.5l5 5L21 8l-5-5-3.5 3.5z" />
        <path d="M7 12l5 5-3 3-5-5 3-3z" />
      </svg>
    ),
  },
  {
    title: 'Fixed-price sales',
    text: 'List and sell instantly at a price you set.',
    icon: (
      <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M20.59 13.41L11 3.83A2 2 0 0 0 9.59 3.24L4 3a1 1 0 0 0-1 1l.24 5.59a2 2 0 0 0 .59 1.41l9.58 9.58a2 2 0 0 0 2.83 0l4.35-4.35a2 2 0 0 0 0-2.82z" />
        <circle cx="7.5" cy="7.5" r="1" />
      </svg>
    ),
  },
  {
    title: 'Cold storage & pickup',
    text: 'Optional logistics matched after you win a lot.',
    icon: (
      <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <rect x="1" y="6" width="15" height="12" rx="1" />
        <path d="M16 10h3l3 3v3h-6z" />
        <circle cx="5.5" cy="18.5" r="1.5" />
        <circle cx="17.5" cy="18.5" r="1.5" />
      </svg>
    ),
  },
  {
    title: 'Invoicing & payouts',
    text: 'Automatic invoices and payout ledger for every sale.',
    icon: (
      <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
        <path d="M14 2v6h6" />
        <path d="M9 13h6" />
        <path d="M9 17h6" />
      </svg>
    ),
  },
];

export function AuthHomePage() {
  return (
    <div className="landing">
      <section className="landing-hero">
        <div className="landing-hero-grid">
          <div>
            <span className="landing-pill">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M17 3a2.85 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5z" />
              </svg>
              Direct from farm to buyer
            </span>
            <h1 className="landing-h1">Sell your harvest at the price it deserves.</h1>
            <p className="landing-lead">
              FarmaTrade connects farmers directly with traders, retailers, and exporters. List a lot as a live
              auction or a fixed-price sale — no commission agents, no opaque pricing.
            </p>
            <div className="landing-cta">
              <Link to="/register?role=farmer" className="btn btn-primary">
                Register as a Farmer
              </Link>
              <Link to="/register?role=buyer" className="btn btn-secondary">
                Register as a Buyer
              </Link>
              <Link to="/login" className="btn btn-secondary">
                Login
              </Link>
            </div>
          </div>
          <img className="landing-image" src={heroHandshake} alt="A farmer and a buyer shaking hands on a deal" />
        </div>
      </section>

      <section className="landing-features">
        {FEATURES.map((feature) => (
          <div key={feature.title} className="card">
            <div className="card-body">
              <div className="feature-icon">{feature.icon}</div>
              <h3>{feature.title}</h3>
              <p>{feature.text}</p>
            </div>
          </div>
        ))}
      </section>
    </div>
  );
}
