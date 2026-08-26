import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import api from "../services/api";

function Settings() {
  const { logout } = useAuth();
  const { showToast } = useToast();
  const navigate = useNavigate();

  // Preferences
  const [theme, setTheme] = useState(
    () => localStorage.getItem("careerpilot.theme") || "light"
  );

  const [notifications, setNotifications] = useState(
    () => localStorage.getItem("careerpilot.notifications") !== "false"
  );

  // Password
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showCurrentPassword, setShowCurrentPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  const [changingPassword, setChangingPassword] = useState(false);
  const [deleting, setDeleting] = useState(false);

  // Save notification preference
  useEffect(() => {
    localStorage.setItem(
      "careerpilot.notifications",
      String(notifications)
    );
  }, [notifications]);

  // Apply theme
 useEffect(() => {
  localStorage.setItem("careerpilot.theme", theme);

  document.documentElement.dataset.theme = theme;
  document.body.dataset.theme = theme;
}, [theme]);

  // Logout
  const handleLogout = () => {
    logout();
    navigate("/login", { replace: true });
  };

  // Change password
  const handleChangePassword = async (event) => {
    event.preventDefault();

    if (!currentPassword.trim()) {
      showToast("Please enter your current password.", "error");
      return;
    }

    if (newPassword.length < 8) {
      showToast(
        "New password must be at least 8 characters.",
        "error"
      );
      return;
    }

    if (newPassword !== confirmPassword) {
      showToast("New passwords do not match.", "error");
      return;
    }

    if (currentPassword === newPassword) {
      showToast(
        "New password must be different from your current password.",
        "error"
      );
      return;
    }

    setChangingPassword(true);

    try {
      await api.put("/profile/password", {
        currentPassword,
        newPassword,
      });

      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");

      showToast(
        "Password changed successfully.",
        "success"
      );
    } catch (error) {
      showToast(
        error.response?.data?.message ||
          "Couldn't change your password.",
        "error"
      );
    } finally {
      setChangingPassword(false);
    }
  };

  // Delete account
  const handleDeleteAccount = async () => {
    const confirmed = window.confirm(
      "Are you sure you want to permanently delete your CareerPilot account?\n\nYour resumes, jobs, matches, roadmaps and account data will also be deleted.\n\nThis action cannot be undone."
    );

    if (!confirmed) {
      return;
    }

    setDeleting(true);

    try {
      await api.delete("/profile");

      logout();

      navigate("/login?deleted=1", {
        replace: true,
      });
    } catch (error) {
      showToast(
        error.response?.data?.message ||
          "Couldn't delete your account.",
        "error"
      );

      setDeleting(false);
    }
  };

  // Reset local preferences
  const resetPreferences = () => {
    setTheme("light");
    setNotifications(true);

    showToast(
      "Preferences restored to default.",
      "success"
    );
  };

  return (
    <main className="page-shell settings-page">
      {/* Header */}
      <div className="section-head">
        <div>
          <p className="eyebrow">PREFERENCES</p>

          <h1>Settings</h1>

          <p>
            Manage your CareerPilot experience, security and account.
          </p>
        </div>
      </div>

      <section className="panel settings-list">

        {/* =========================
            APPEARANCE
        ========================== */}
        <div className="settings-section-title">
          Appearance
        </div>

        <div className="settings-row">
          <div className="settings-info">
            <strong>Theme</strong>

            <span>
              Choose how CareerPilot looks across your dashboard.
            </span>
          </div>

          <div
            className="theme-switcher"
            role="group"
            aria-label="Theme"
          >
            <button
              type="button"
              className={theme === "light" ? "selected" : ""}
              onClick={() => setTheme("light")}
            >
              ☀ Light
            </button>

            <button
              type="button"
              className={theme === "dark" ? "selected" : ""}
              onClick={() => setTheme("dark")}
            >
              ☾ Dark
            </button>
          </div>
        </div>

        <div className="settings-row">
          <div className="settings-info">
            <strong>Career updates</strong>

            <span>
              Show progress, completion and career-related
              notifications.
            </span>
          </div>

          <button
            type="button"
            className={`toggle ${
              notifications ? "on" : ""
            }`}
            onClick={() =>
              setNotifications((value) => !value)
            }
            aria-label="Toggle career updates"
            aria-pressed={notifications}
          >
            <span />
          </button>
        </div>

        <div className="settings-row">
          <div className="settings-info">
            <strong>Reset preferences</strong>

            <span>
              Restore theme and notification preferences to
              their default values.
            </span>
          </div>

          <button
            type="button"
            className="secondary-btn"
            onClick={resetPreferences}
          >
            Reset
          </button>
        </div>

        {/* =========================
            SECURITY
        ========================== */}
        <div className="settings-section-title">
          Security
        </div>

        <form
          className="settings-password-form"
          onSubmit={handleChangePassword}
        >
          <label>
            Current password

            <div className="settings-password-wrapper">
              <input
                type={
                  showCurrentPassword
                    ? "text"
                    : "password"
                }
                value={currentPassword}
                onChange={(event) =>
                  setCurrentPassword(event.target.value)
                }
                placeholder="Enter current password"
                autoComplete="current-password"
                required
              />

              <button
                type="button"
                className="settings-password-toggle"
                onClick={() =>
                  setShowCurrentPassword(
                    (value) => !value
                  )
                }
              >
                {showCurrentPassword ? "Hide" : "Show"}
              </button>
            </div>
          </label>

          <label>
            New password

            <div className="settings-password-wrapper">
              <input
                type={
                  showNewPassword
                    ? "text"
                    : "password"
                }
                value={newPassword}
                onChange={(event) =>
                  setNewPassword(event.target.value)
                }
                placeholder="At least 8 characters"
                autoComplete="new-password"
                minLength={8}
                required
              />

              <button
                type="button"
                className="settings-password-toggle"
                onClick={() =>
                  setShowNewPassword(
                    (value) => !value
                  )
                }
              >
                {showNewPassword ? "Hide" : "Show"}
              </button>
            </div>
          </label>

          <label>
            Confirm new password

            <div className="settings-password-wrapper">
              <input
                type={
                  showConfirmPassword
                    ? "text"
                    : "password"
                }
                value={confirmPassword}
                onChange={(event) =>
                  setConfirmPassword(event.target.value)
                }
                placeholder="Re-enter new password"
                autoComplete="new-password"
                minLength={8}
                required
              />

              <button
                type="button"
                className="settings-password-toggle"
                onClick={() =>
                  setShowConfirmPassword(
                    (value) => !value
                  )
                }
              >
                {showConfirmPassword
                  ? "Hide"
                  : "Show"}
              </button>
            </div>
          </label>

          <button
            type="submit"
            className="primary-btn settings-save-password"
            disabled={changingPassword}
          >
            {changingPassword
              ? "Changing..."
              : "Change password"}
          </button>
        </form>

        {/* =========================
            ACCOUNT
        ========================== */}
        <div className="settings-section-title">
          Account
        </div>

        <div className="settings-row">
          <div className="settings-info">
            <strong>Sign out</strong>

            <span>
              End your current CareerPilot session on this
              device.
            </span>
          </div>

          <button
            type="button"
            className="secondary-btn"
            onClick={handleLogout}
          >
            Logout
          </button>
        </div>

        <div className="settings-row danger-row">
          <div className="settings-info">
            <strong>Delete account</strong>

            <span>
              Permanently delete your CareerPilot account
              and all associated data.
            </span>
          </div>

          <button
            type="button"
            className="danger-btn"
            onClick={handleDeleteAccount}
            disabled={deleting}
          >
            {deleting
              ? "Deleting..."
              : "Delete account"}
          </button>
        </div>
      </section>
    </main>
  );
}

export default Settings;