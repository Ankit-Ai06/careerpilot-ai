import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";

import Sidebar from "./components/Sidebar";
import ProtectedRoute from "./components/ProtectedRoute";

import Dashboard from "./pages/Dashboard";
import Login from "./pages/Login";
import Register from "./pages/Register";
import ResumeAnalyzer from "./pages/ResumeAnalyzer";
import JobMatch from "./pages/JobMatch";
import Roadmap from "./pages/RoadMap";
import Interview from "./pages/Interview";
import Settings from "./pages/Settings";

function ProtectedLayout() {
  return (
    <ProtectedRoute>
      <div className="app">
        <Sidebar />

        <div className="main-content">
          <Routes>
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/resume" element={<ResumeAnalyzer />} />
            <Route path="/job-match" element={<JobMatch />} />
            <Route path="/roadmap" element={<Roadmap />} />
            <Route path="/interview" element={<Interview />} />
            <Route path="/settings" element={<Settings />} />

            <Route
              path="*"
              element={<Navigate to="/dashboard" replace />}
            />
          </Routes>
        </div>
      </div>
    </ProtectedRoute>
  );
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />

        <Route path="/*" element={<ProtectedLayout />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;