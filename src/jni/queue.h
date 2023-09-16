/** phiola/Android: queue
2023, Simon Zolin */

JNIEXPORT jlong JNICALL
Java_com_github_stsaz_phiola_Phiola_quNew(JNIEnv *env, jobject thiz)
{
	struct phi_queue_conf c = {};
	return (ffsize)x->queue->create(&c);
}

JNIEXPORT void JNICALL
Java_com_github_stsaz_phiola_Phiola_quDestroy(JNIEnv *env, jobject thiz, jlong q)
{
	x->queue->destroy((void*)q);
}

#define QUADD_RECURSE  1

JNIEXPORT void JNICALL
Java_com_github_stsaz_phiola_Phiola_quAdd(JNIEnv *env, jobject thiz, jlong q, jobjectArray jurls, jint flags)
{
	dbglog("%s: enter", __func__);
	jstring js = NULL;
	const char *fn = NULL;
	ffsize n = jni_arr_len(jurls);
	for (uint i = 0;  i != n;  i++) {
		jni_sz_free(fn, js);
		js = jni_joa_i(jurls, i);
		fn = jni_sz_js(js);

		struct phi_queue_entry qe = {
			.conf.ifile.name = ffsz_dup(fn),
		};
		x->queue->add((void*)q, &qe);
	}

	jni_sz_free(fn, js);
	dbglog("%s: exit", __func__);
}

JNIEXPORT jstring JNICALL
Java_com_github_stsaz_phiola_Phiola_quEntry(JNIEnv *env, jobject thiz, jlong q, jint i)
{
	struct phi_queue_entry *qe = x->queue->ref((void*)q, i);
	const char *url = qe->conf.ifile.name;
	jstring s = jni_js_sz(url);
	x->queue->unref(qe);
	return s;
}

#define QUCOM_CLEAR  1
#define QUCOM_REMOVE_I  2
#define QUCOM_COUNT  3

JNIEXPORT jint JNICALL
Java_com_github_stsaz_phiola_Phiola_quCmd(JNIEnv *env, jobject thiz, jlong jq, jint cmd, jint i)
{
	phi_queue_id q = (void*)jq;

	switch (cmd) {
	case QUCOM_CLEAR:
		x->queue->clear(q);
		break;

	case QUCOM_REMOVE_I:
		x->queue->remove_at(q, i, 1);
		break;

	case QUCOM_COUNT:
		return x->queue->count(q);
	}
	return 0;
}

JNIEXPORT jobject JNICALL
Java_com_github_stsaz_phiola_Phiola_quMeta(JNIEnv *env, jobject thiz, jlong jq, jint i)
{
	phi_queue_id q = (void*)jq;
	struct phi_queue_entry *qe = x->queue->ref(q, i);
	jobject jmeta = meta_create(env, &qe->conf.meta, qe->conf.ifile.name, qe->length_msec);
	x->queue->unref(qe);
	return jmeta;
}

enum {
	QUFILTER_URL = 1,
	QUFILTER_META = 2,
};

JNIEXPORT jint JNICALL
Java_com_github_stsaz_phiola_Phiola_quFilter(JNIEnv *env, jobject thiz, jlong q, jstring jfilter, jint flags)
{
	return 0;
}

JNIEXPORT jint JNICALL
Java_com_github_stsaz_phiola_Phiola_quLoad(JNIEnv *env, jobject thiz, jlong q, jstring jfilepath)
{
	dbglog("%s: enter", __func__);
	const char *fn = jni_sz_js(jfilepath);
	struct phi_queue_entry qe = {
		.conf.ifile.name = ffsz_dup(fn),
	};
	x->queue->add((void*)q, &qe);
	jni_sz_free(fn, jfilepath);
	dbglog("%s: exit", __func__);
	return 0;
}

JNIEXPORT jboolean JNICALL
Java_com_github_stsaz_phiola_Phiola_quSave(JNIEnv *env, jobject thiz, jlong q, jstring jfilepath)
{
	dbglog("%s: enter", __func__);
	const char *fn = jni_sz_js(jfilepath);
	x->queue->save((void*)q, fn);
	jni_sz_free(fn, jfilepath);
	dbglog("%s: exit", __func__);
	return 1;
}
