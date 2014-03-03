package aquestalk2;

public class AquesTalk2 {
	static {
		System.loadLibrary("AquesTalk2");
	}
	/**
	 * �����L���񂩂特���f�[�^�𐶐����܂��B
	 * <p>���b���x�͒ʏ�̑��x��100�Ƃ��āA50 - 300 �̊ԂŎw�肵�܂�(�P�ʂ�%)�B</p>
	 * @param kanaText �����L����(UTF-8)
	 * @param speed ���b���x(%)
	 * @param phontDat Phont�f�[�^   �f�t�H���g�̂�p����Ƃ���null���w��
	 * @return wav�t�H�[�}�b�g�̃f�[�^   �G���[���ɂ�,�����P�ŁA�擪�ɃG���[�R�[�h���Ԃ����
	 */
	public static byte[] synthe(String kanaText, int speed, byte[] phontDat)
	{
		return new AquesTalk2().syntheWav(kanaText, speed, phontDat);
	}
	/**
	 * �����L���񂩂特���f�[�^�𐶐����܂��BJNI����(native�C���q)
	 * <p>���b���x�͒ʏ�̑��x��100�Ƃ��āA50 - 300 �̊ԂŎw�肵�܂�(�P�ʂ�%)�B</p>
	 * @param kanaText �����L����(UTF-8)
	 * @param speed ���b���x(%)
	 * @param phontDat Phont�f�[�^   �f�t�H���g�̂�p����Ƃ���null���w��
	 * @return wav�t�H�[�}�b�g�̃f�[�^  �G���[���ɂ�,�����P�ŁA�擪�ɃG���[�R�[�h���Ԃ���� 
	 */
	public synchronized native byte[] syntheWav(String kanaText, int speed, byte[] phontDat);
}
/*
�G���[�R�[�h�ꗗ
	100 ���̑��̃G���[
	101 �������s��
	102 �����L����ɖ���`�̓ǂ݋L�����w�肳�ꂽ
	103 �C���f�[�^�̎��Ԓ����}�C�i�X�Ȃ��Ă���
	104 �����G���[(����`�̋�؂�R�[�h���o�j
	105 �����L����ɖ���`�̓ǂ݋L�����w�肳�ꂽ
	106 �����L����̃^�O�̎w�肪�������Ȃ�
	107 �^�O�̒������������z���Ă���i�܂���[>]���݂���Ȃ��j
	108 �^�O���̒l�̎w�肪�������Ȃ�
	109 WAVE �Đ����ł��Ȃ��i�T�E���h�h���C�o�֘A�̖��j
	110 WAVE �Đ����ł��Ȃ��i�T�E���h�h���C�o�֘A�̖�� �񓯊��Đ��j
	111 �������ׂ��f�[�^���Ȃ�
	-38 �����L���񂪒�������
	-37 �P�̃t���[�Y���̓ǂ݋L������������
	-36 �����L���񂪒����i�����o�b�t�@�I�[�o�[1�j
	-35 �q�[�v�������s��
	-34 �����L���񂪒����i�����o�b�t�@�I�[�o�[1�j
	-16~-24 Phont �f�[�^���������Ȃ�
*/