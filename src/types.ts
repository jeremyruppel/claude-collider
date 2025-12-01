export enum ServerState {
  Stopped = "stopped",
  Booting = "booting",
  Running = "running",
}

export interface PendingOperation {
  resolve: (value: string) => void
  reject: (error: Error) => void
  timeout: ReturnType<typeof setTimeout>
}
