/** phiola: executor: 'remote' command
2023, Simon Zolin */

static int remote_help()
{
	static const char s[] = "\
Show file meta data:\n\
    phiola remote CMD...\n\
\n\
Commands:\n\
  start INPUT       Add track and play\n\
  clear             Clear playlist\n\
  play              Play\n\
  next              Play next track\n\
  previous          Play previous track\n\
  stop              Stop all tracks\n\
  quit              Exit\n\
";
	ffstdout_write(s, FFS_LEN(s));
	x->exit_code = 0;
	return 1;
}

struct cmd_remote {
	ffvec cmd;
};

static int remote_cmd(struct cmd_remote *r, ffstr s)
{
	if (ffstr_findchar(&s, '"') >= 0) {
		errlog("double quotes can not be used");
		return 1;
	}

	if (ffstr_findchar(&s, ' ') >= 0)
		ffvec_addfmt(&r->cmd, "\"%S\" ", &s);
	else
		ffvec_addfmt(&r->cmd, "%S ", &s);
	return 0;
}

static int remote_action(struct cmd_remote *r)
{
	const phi_remote_cl_if *rcl = x->core->mod("remote.client");
	if (rcl->cmd(NULL, *(ffstr*)&r->cmd))
		return 1;

	x->core->sig(PHI_CORE_STOP);
	x->exit_code = 0;
	return 0;
}

static int remote_fin(struct cmd_remote *r)
{
	return 0;
}

#define O(m)  (void*)FF_OFF(struct cmd_remote, m)
static const struct ffarg cmd_remote[] = {
	{ "-help",		0,		remote_help },
	{ "\0\1",		'S',	remote_cmd },
	{ "",			0,		remote_fin },
};
#undef O

static void cmd_remote_free(struct cmd_remote *r)
{
	ffvec_free(&r->cmd);
	ffmem_free(r);
}

struct ffarg_ctx cmd_remote_init(void *obj)
{
	return SUBCMD_INIT(ffmem_new(struct cmd_remote), cmd_remote_free, remote_action, cmd_remote);
}
