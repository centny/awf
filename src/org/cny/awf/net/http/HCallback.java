package org.cny.awf.net.http;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import org.cny.jwf.hook.Hooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.os.Handler;
import android.os.Message;

import com.google.gson.Gson;

public interface HCallback {
	void onProcess(CBase c, PIS pis, float rate);

	OutputStream createO(CBase c, HResp res) throws Exception;

	void onProcess(CBase c, float rate);

	void onProcEnd(CBase c, HResp res, OutputStream o) throws Exception;

	void onSuccess(CBase c, HResp res) throws Exception;

	void onError(CBase c, Throwable err) throws Exception;

	public static abstract class HDataCallback implements HCallback {
		ByteArrayOutputStream buf = new ByteArrayOutputStream();

		@Override
		public void onProcess(CBase c, PIS pis, float rate) {

		}

		@Override
		public OutputStream createO(CBase c, HResp res) throws Exception {
			this.buf.reset();
			return this.buf;
		}

		@Override
		public void onProcess(CBase c, float rate) {

		}

		@Override
		public void onProcEnd(CBase c, HResp res, OutputStream o)
				throws Exception {
		}

		@Override
		public void onSuccess(CBase c, HResp res) throws Exception {
			String data = new String(this.buf.toByteArray(), res.enc);
			// sending hook
			if (Hooks.call(HDataCallback.class, "onSuccess", c, res, data) < 1) {
				this.onSuccess(c, res, data);
			}
		}

		public abstract void onSuccess(CBase c, HResp res, String data)
				throws Exception;
	}

	public static abstract class HCacheCallback extends HDataCallback {

		@Override
		public void onError(CBase c, Throwable err) throws Exception {
			String cache = c.readCache();
			if (Hooks.call(HCacheCallback.class, "onError", c, cache, err) < 1) {
				this.onError(c, cache, err);
			}
		}

		public abstract void onError(CBase c, String cache, Throwable err)
				throws Exception;
	}

	public abstract class GDataCallback<T> extends HDataCallback {
		protected Class<?> cls;
		protected Gson gs = new Gson();

		public GDataCallback(Class<?> cls) {
			this.cls = cls;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void onSuccess(CBase c, HResp res, String data) throws Exception {
			T val;
			if (data == null || data.isEmpty()) {
				val = null;
			} else {
				val = (T) this.gs.fromJson(data.trim(), this.cls);
			}
			if (Hooks.call(HCacheCallback.class, "onSuccess", c, res, val) < 1) {
				this.onSuccess(c, res, val);
			}
		}

		public abstract void onSuccess(CBase c, HResp res, T data)
				throws Exception;
	}

	public abstract class GCacheCallback<T> extends HCacheCallback {
		protected Class<?> cls;
		protected Gson gs = new Gson();

		public GCacheCallback(Class<?> cls) {
			this.cls = cls;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void onError(CBase c, String cache, Throwable err)
				throws Exception {
			T val;
			if (cache == null || cache.isEmpty()) {
				val = null;
			} else {
				val = (T) this.gs.fromJson(cache.trim(), this.cls);
			}
			if (Hooks.call(HCacheCallback.class, "onError", c, val, err) < 1) {
				this.onError(c, val, err);
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		public void onSuccess(CBase c, HResp res, String data) throws Exception {
			T val;
			if (data == null || data.isEmpty()) {
				val = null;
			} else {
				val = (T) this.gs.fromJson(data.trim(), this.cls);
			}
			if (Hooks.call(HCacheCallback.class, "onSuccess", c, res, val) < 1) {
				this.onSuccess(c, res, val);
			}
		}

		public abstract void onError(CBase c, T cache, Throwable err)
				throws Exception;

		public abstract void onSuccess(CBase c, HResp res, T data)
				throws Exception;
	}

	public static class HandlerCallback implements HCallback {
		private static final Logger L = LoggerFactory
				.getLogger(HandlerCallback.class);

		public static int vvv = 0;
		protected static Handler H = new Handler() {

			@Override
			public void dispatchMessage(Message msg) {
				try {
					Object[] args = (Object[]) msg.obj;
					HCallback tg = (HCallback) args[0];
					switch (msg.what) {
					case 0:
						tg.onProcess((CBase) args[1], (PIS) args[2],
								(Float) args[3]);
						break;
					case 1:
						tg.onProcess((CBase) args[1], (Float) args[2]);
						break;
					case 2:
						tg.onSuccess((CBase) args[1], (HResp) args[2]);
						break;
					case 3:
						tg.onError((CBase) args[1], (Throwable) args[2]);
						break;
					default:
						throw new Exception("invalid message type for"
								+ msg.what);
					}
				} catch (Exception e) {
					L.warn("exec HCallback({}) err", msg.what, e);
				}
			}

		};

		protected HCallback target;

		public HandlerCallback(HCallback target) {
			this.target = target;
		}

		@Override
		public void onProcess(CBase c, PIS pis, float rate) {
			Message msg = new Message();
			msg.what = 0;
			msg.obj = new Object[] { this.target, c, pis, (Float) rate };
			H.sendMessage(msg);
		}

		@Override
		public void onProcess(CBase c, float rate) {
			Message msg = new Message();
			msg.what = 1;
			msg.obj = new Object[] { this.target, c, (Float) rate };
			H.sendMessage(msg);
		}

		@Override
		public void onSuccess(CBase c, HResp res) throws Exception {
			Message msg = new Message();
			msg.what = 2;
			msg.obj = new Object[] { this.target, c, res };
			H.sendMessage(msg);
		}

		@Override
		public void onError(CBase c, Throwable err) throws Exception {
			Message msg = new Message();
			msg.what = 3;
			msg.obj = new Object[] { this.target, c, err };
			H.sendMessage(msg);
		}

		@Override
		public OutputStream createO(CBase c, HResp res) throws Exception {
			return this.target.createO(c, res);
		}

		@Override
		public void onProcEnd(CBase c, HResp res, OutputStream o)
				throws Exception {
			this.target.onProcEnd(c, res, o);
		}
	}
}
