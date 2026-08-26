import { useEffect, useState } from "react";
import { NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import api from "../services/api";

function Sidebar() {
  const navigate = useNavigate();
  const { logout } = useAuth();

  const [profile, setProfile] = useState({
    name: "Ankit",
    email: "",
  });

  const [menuOpen, setMenuOpen] = useState(false);

  useEffect(() => {
    let mounted = true;

    api
      .get("/profile")
      .then((response) => {
        if (mounted) {
          setProfile(response.data);
        }
      })
      .catch(() => {});

    return () => {
      mounted = false;
    };
  }, []);

  const firstLetter =
    profile.name?.trim()?.charAt(0)?.toUpperCase() || "A";

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  const go = (path) => {
    setMenuOpen(false);
    navigate(path);
  };

  return (
    <aside className="sidebar">
      <div className="logo">
        <span className="logo-icon">✦</span>

        <span>
          CareerPilot <strong>AI</strong>
        </span>
      </div>

      <nav className="sidebar-nav">
        <NavLink
          to="/dashboard"
          className={({ isActive }) =>
            isActive ? "nav-item active" : "nav-item"
          }
        >
          <span>⌂</span>
          Dashboard
        </NavLink>

        <NavLink
          to="/resume"
          className={({ isActive }) =>
            isActive ? "nav-item active" : "nav-item"
          }
        >
          <span>▣</span>
          Resume Analyzer
        </NavLink>

        <NavLink
          to="/job-match"
          className={({ isActive }) =>
            isActive ? "nav-item active" : "nav-item"
          }
        >
          <span>⌕</span>
          Job Match
        </NavLink>

        <NavLink
          to="/roadmap"
          className={({ isActive }) =>
            isActive ? "nav-item active" : "nav-item"
          }
        >
          <span>◆</span>
          Skill Roadmap
        </NavLink>

        <NavLink
          to="/interview"
          className={({ isActive }) =>
            isActive ? "nav-item active" : "nav-item"
          }
        >
          <span>◉</span>
          AI Interview
        </NavLink>
      </nav>

      <div className="sidebar-user-area">
        {menuOpen && (
          <div className="profile-menu">
            <div className="profile-menu-head">
              <div className="profile-avatar">
                {firstLetter}
              </div>

              <div>
                <strong>{profile.name}</strong>

                <small>
                  {profile.email || "CSE Student"}
                </small>
              </div>
            </div>

            {/* My Profile -> Dashboard */}
            <button
              className="profile-menu-item"
              onClick={() => go("/dashboard")}
            >
              <span>♟</span>

              <div>
                <strong>My Profile</strong>
                <small>View your career profile</small>
              </div>
            </button>

            {/* Settings */}
            <button
              className="profile-menu-item"
              onClick={() => go("/settings")}
            >
              <span>⚙</span>

              <div>
                <strong>Settings</strong>
                <small>Manage your preferences</small>
              </div>
            </button>

            {/* Logout */}
            <button
              className="profile-menu-item logout-item"
              onClick={handleLogout}
            >
              <span>↪</span>

              <div>
                <strong>Logout</strong>
                <small>Sign out of CareerPilot AI</small>
              </div>
            </button>
          </div>
        )}

        <button
          type="button"
          className={`sidebar-footer ${
            menuOpen ? "open" : ""
          }`}
          onClick={() => setMenuOpen((value) => !value)}
          aria-expanded={menuOpen}
        >
          <div className="user-avatar">
            {firstLetter}
          </div>

          <div className="sidebar-user-copy">
            <strong>{profile.name}</strong>
            <span>CSE Student</span>
          </div>

          <span className="profile-chevron">
            {menuOpen ? "⌃" : "⌄"}
          </span>
        </button>
      </div>
    </aside>
  );
}

export default Sidebar;