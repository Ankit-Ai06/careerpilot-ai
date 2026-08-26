function Toast({ type = "success", message, onClose }) {
  return (
    <div className={`toast toast-${type}`}>
      <div className="toast-icon">
        {type === "success" && "✓"}
        {type === "error" && "!"}
        {type === "warning" && "!"}
        {type === "info" && "i"}
      </div>

      <div className="toast-content">
        <strong>
          {type === "success" && "Success"}
          {type === "error" && "Something went wrong"}
          {type === "warning" && "Warning"}
          {type === "info" && "Information"}
        </strong>

        <p>{message}</p>
      </div>

      <button
        className="toast-close"
        onClick={onClose}
        aria-label="Close notification"
      >
        ×
      </button>
    </div>
  );
}

export default Toast;