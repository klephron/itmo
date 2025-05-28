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

  function login() {
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
        switch (e.status) {
          case 401:
            alert("Login failed");
            break;
          default:
            alert(await e);
        }
      });
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
          >
            Login
          </button>
        </form>
      </div>
    </>
  );
}
