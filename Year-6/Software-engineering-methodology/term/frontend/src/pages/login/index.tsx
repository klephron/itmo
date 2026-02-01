import { useState } from "react";
import { useClient } from "@state/client";
import { useUser } from "@state/user";
import { useNavigate } from "react-router";
import { useToken } from "@state/token";
import styles from "./index.module.scss";

export default function LoginPage() {
  const navigate = useNavigate();

  const client = useClient((state) => state.client);

  const setUser = useUser((state) => state.update);
  const setToken = useToken((state) => state.update);

  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, errorSet] = useState<string | null>(null);
  const [loading, loadingSet] = useState(false);

  function login() {
    errorSet(null);
    if (!username.trim() || !password.trim()) {
      errorSet("Username and password are required");
      return;
    }
    loadingSet(true);
    client.auth
      .login(
        {
          username: username,
          password: password,
        },
        { secure: false }
      )
      .then((r) => {
        setUser({
          id: r.data.id,
          username: r.data.username,
          present: true,
        });

        setToken({
          accessToken: r.data.accessToken,
          refreshToken: "",
          tokenType: "Bearer",
          issueAtMS: r.data.iat,
          expiresInMS: r.data.expiresIn * 1000,
          present: true,
        });

        navigate("/");
      })
      .catch(async (e) => {
        if (e?.status === 401) {
          errorSet("Invalid username or password");
          return;
        }
        const body = typeof e?.error === "string" ? e.error : e?.rawBody;
        errorSet(body || e?.message || "Login failed");
      })
      .finally(() => loadingSet(false));
  }

  return (
    <>
      <div className={styles.root}>
        <form>
          <div className={styles.title}>
            <h2>ITMO PAI</h2>
          </div>
          <div className={styles.content}>
            <label>Username:</label>
            <input type="text" value={username} onChange={(e) => setUsername(e.target.value)} />
            <label>Password:</label>
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
          </div>
          <button
            onClick={(e) => {
              e.preventDefault();
              login();
            }}
            disabled={loading}
          >
            {loading ? "Signing in..." : "Login"}
          </button>
          {error ? <div className={styles.error}>{error}</div> : null}
        </form>
      </div>
    </>
  );
}
